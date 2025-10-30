package com.example.air_o_walk.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.air_o_walk.databinding.FragmentHomeBinding;

import java.util.Random;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private int numeroRandom;

    private TextView minor;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //final TextView textView = binding.textHome;
        //homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        minor = binding.textMinor;
        return root;


    }

    public void botonBuscarDispositivosBTLEPulsado( View v ) {
        numeroRandom = 0;
        minor.setText(numeroRandom);
        numeroRandom++;
    } // ()

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}