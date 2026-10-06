package com.example.mad_nnm23cs124.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.mad_nnm23cs124.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

@RequiresApi(api = Build.VERSION_CODES.P)
public class MoreBottomSheetDialog extends BottomSheetDialogFragment {

    private OnMenuSelectedListener listener;

    public interface OnMenuSelectedListener {
        void onMenuSelected(String option);
    }

    public void setOnMenuSelectedListener(OnMenuSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_more_sheet, container, false);

        view.findViewById(R.id.btnMenuKeyboard).setOnClickListener(v -> {
            if (listener != null) listener.onMenuSelected("Keyboard");
            dismiss();
        });

        view.findViewById(R.id.btnMenuMedia).setOnClickListener(v -> {
            if (listener != null) listener.onMenuSelected("Media");
            dismiss();
        });

        view.findViewById(R.id.btnMenuPresenter).setOnClickListener(v -> {
            if (listener != null) listener.onMenuSelected("Presenter");
            dismiss();
        });

        view.findViewById(R.id.btnMenuScanner).setOnClickListener(v -> {
            if (listener != null) listener.onMenuSelected("Scanner");
            dismiss();
        });

        view.findViewById(R.id.btnMenuJiggler).setOnClickListener(v -> {
            if (listener != null) listener.onMenuSelected("Jiggler");
            dismiss();
        });

        view.findViewById(R.id.btnMenuSettings).setOnClickListener(v -> {
            if (listener != null) listener.onMenuSelected("Settings");
            dismiss();
        });

        return view;
    }
}
