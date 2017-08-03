/*
 * 	Copyright (c) 2017. Toshi Browser, Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.toshi.util;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.toshi.R;
import com.toshi.crypto.util.TypeConverter;
import com.toshi.exception.InvalidQrCode;
import com.toshi.exception.InvalidQrCodePayment;
import com.toshi.model.local.QrCodePayment;
import com.toshi.view.BaseApplication;

import java.math.BigInteger;

import rx.Single;

public class QrCode {

    private static final String PAY_TYPE = "pay";
    private static final String ADD_TYPE = "add";
    private static final String EXTERNAL_URL_PREFIX = "ethereum:";
    private static final String IBAN_URL_PREFIX = "iban:";
    private static final String VALUE = "value";
    private static final String MEMO = "memo";

    private String payload;

    public QrCode(final String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return this.payload;
    }

    public @QrCodeType.Type int getQrCodeType() {
        final String baseUrl = BaseApplication.get().getString(R.string.qr_code_base_url);
        if (this.payload.startsWith(baseUrl + PAY_TYPE)) {
            return QrCodeType.PAY;
        } else if (this.payload.startsWith(baseUrl + ADD_TYPE)) {
            return QrCodeType.ADD;
        } else if (this.payload.startsWith(EXTERNAL_URL_PREFIX)) {
            return getTypeForEthereumAddress();
        } else if (this.payload.startsWith(IBAN_URL_PREFIX)) {
            return getTypeForIbanAddress();
        } else if (isValidEthereumAddress(this.payload)) {
            final String encoded = Integer.toHexString(Integer.parseInt(this.payload, 36));
            LogUtil.print(getClass(), encoded       );
            return QrCodeType.PAYMENT_ADDRESS;
        } else {
            return QrCodeType.INVALID;
        }
    }

    private int getTypeForEthereumAddress() {
        return isPaymentAddressQrCode()
                ? QrCodeType.PAYMENT_ADDRESS
                : QrCodeType.EXTERNAL_PAY;
    }

    private int getTypeForIbanAddress() {
        final String ibanAddress = cleanIbanAddress().split("\\?")[0];
        if (ibanAddress.length() < 30) return QrCodeType.INVALID;

        return isPaymentAddressQrCode()
                ? QrCodeType.PAYMENT_ADDRESS
                : QrCodeType.EXTERNAL_PAY;
    }

    private boolean isValidEthereumAddress(final String address) {
        if (address.startsWith("0x")) {
            return address.length() == 42;
        }
        return address.length() == 40;
    }

    /* package */ String getPayloadAsPaymentAddress() {
        if (this.payload.startsWith(IBAN_URL_PREFIX)) {
            final String hexAddress = new BigInteger(cleanIbanAddress(), 36).toString(16);
            return TypeConverter.toJsonHex(hexAddress);
        }
        return this.payload;
    }

    public String getUsername() throws InvalidQrCode {
        try {
            final String username = Uri.parse(this.payload).getLastPathSegment();
            if (username == null) throw new InvalidQrCode();
            final String usernameWithoutPrefix = username.startsWith("@")
                    ? username.replaceFirst("@", "")
                    : null;
            if (usernameWithoutPrefix != null) return usernameWithoutPrefix;
            else throw new InvalidQrCode();
        } catch (UnsupportedOperationException e) {
            throw new InvalidQrCode(e);
        }
    }

    public QrCodePayment getPayment() throws InvalidQrCodePayment {
        try {
            final String username = getUsername();
            final QrCodePayment payment = getPaymentWithParams()
                    .setUsername(username);
            if (payment.isValid()) return payment;
            else throw new InvalidQrCodePayment();
        } catch (UnsupportedOperationException | InvalidQrCode e) {
            throw new InvalidQrCodePayment(e);
        }
    }

    public QrCodePayment getExternalPayment() throws InvalidQrCodePayment {
        try {
            final String baseUrl = String.format("%s%s/", BaseApplication.get().getString(R.string.qr_code_base_url), PAY_TYPE);
            this.payload = this.payload.replaceFirst(EXTERNAL_URL_PREFIX, baseUrl);
            final String address = Uri.parse(this.payload).getLastPathSegment();
            if (address == null) throw new InvalidQrCodePayment();
            return getPaymentWithParams()
                    .setAddress(address);
        } catch (UnsupportedOperationException e) {
            throw new InvalidQrCodePayment(e);
        }
    }

    private QrCodePayment getPaymentWithParams() throws UnsupportedOperationException {
        final Uri uri = Uri.parse(this.payload);
        final String value = uri.getQueryParameter(VALUE);
        final String memo = uri.getQueryParameter(MEMO);
        return new QrCodePayment()
                .setValue(value)
                .setMemo(memo);
    }

    public static Single<Bitmap> generateAddQrCode(final String username) {
        final String baseUrl = BaseApplication.get().getString(R.string.qr_code_base_url);
        final String addParams = getAddUrl(username);
        final String url = String.format("%s%s", baseUrl, addParams);
        return ImageUtil.generateQrCode(url);
    }

    private static String getAddUrl(final String username) {
        return BaseApplication
                .get()
                .getString(
                        R.string.qr_code_add_url,
                        ADD_TYPE,
                        username
                );
    }

    public static Single<Bitmap> generatePayQrCode(final String username,
                                                   final String value,
                                                   final String memo) {
        final String baseUrl = BaseApplication.get().getString(R.string.qr_code_base_url);
        final String payParams = memo != null
                ? getPayUrl(username, value, memo)
                : getPayUrl(username, value);
        final String url = String.format("%s%s", baseUrl, payParams);
        return ImageUtil.generateQrCode(url);
    }

    private static String getPayUrl(final String username,
                                    final String value,
                                    final String memo) {
        return BaseApplication
                .get()
                .getString(
                        R.string.qr_code_pay_url,
                        PAY_TYPE,
                        username,
                        value,
                        memo
                );
    }

    private static String getPayUrl(final String username,
                                    final String value) {
        return BaseApplication
                .get()
                .getString(
                        R.string.qr_code_pay_url_without_memo,
                        PAY_TYPE,
                        username,
                        value
                );
    }

    public static Single<Bitmap> generatePaymentAddressQrCode(final String paymentAddress) {
        final String url = String.format("%s%s", EXTERNAL_URL_PREFIX, paymentAddress);
        return ImageUtil.generateQrCode(url);
    }

    private boolean isPaymentAddressQrCode() {
        return !this.payload.contains("?");
    }

    @NonNull
    private String cleanIbanAddress() {
        // Strip off "iban:XE**"
        return this.payload.substring(9);
    }
}
