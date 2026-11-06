package com.dantsu.thermalprinter.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new ReceiptFragment();
            case 1: return new TagsEditorFragment();
            case 2: return new BarcodeFragment();
            case 3: return new ImageStudioFragment();
            case 4: default: return new SettingsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
