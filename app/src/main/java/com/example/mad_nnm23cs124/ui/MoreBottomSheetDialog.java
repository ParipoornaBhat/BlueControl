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

        bindOption(view, R.id.btnMenuBluetooth, "Bluetooth");
        bindOption(view, R.id.btnMenuFullscreen, "Fullscreen");
        bindOption(view, R.id.btnMenuAdjustSize, "AdjustSize");
        bindOption(view, R.id.btnMenuScanner, "Scanner");
        bindOption(view, R.id.btnMenuJiggler, "Jiggler");
        bindOption(view, R.id.btnMenuSettings, "Settings");

        return view;
    }

    private void bindOption(View root, int viewId, String option) {
        View v = root.findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(view -> {
                if (listener != null) listener.onMenuSelected(option);
                dismiss();
            });
        }
    }
}
