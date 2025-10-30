package com.example.air_o_walk.ui.extra;



import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.air_o_walk.databinding.FragmentExtraBinding;

public class ExtraFragment extends Fragment {

    private FragmentExtraBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ExtraViewModel homeViewModel =
                new ViewModelProvider(this).get(ExtraViewModel.class);

        binding = FragmentExtraBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textExtra;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}