package com.example.air_o_walk.ui.extra;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ExtraViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ExtraViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is extra fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}