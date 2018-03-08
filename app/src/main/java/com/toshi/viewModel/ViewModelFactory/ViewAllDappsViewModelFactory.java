/*
 * 	Copyright (c) 2017. Toshi Inc
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

package com.toshi.viewModel.ViewModelFactory;

import android.arch.lifecycle.ViewModelProvider;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.toshi.viewModel.ViewAllDappsViewModel;

public class ViewAllDappsViewModelFactory implements ViewModelProvider.Factory {
    private Intent intent;

    public ViewAllDappsViewModelFactory(final Intent intent) {
        this.intent = intent;
    }

    @NonNull
    @Override
    public ViewAllDappsViewModel create(@NonNull Class modelClass) {
        return new ViewAllDappsViewModel(this.intent);
    }
}
