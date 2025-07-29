package com.aefyr.sai.ui.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.DonateAdapter;
import com.aefyr.sai.billing.BillingManager;
import com.aefyr.sai.billing.DefaultBillingManager;

public class DonateFragment extends SaiBaseFragment implements DonateAdapter.OnProductInteractionListener {

    private BillingManager mBillingManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBillingManager = DefaultBillingManager.getInstance(requireContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recycler = findViewById(R.id.rv_donate);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        DonateAdapter adapter = new DonateAdapter(requireContext(), mBillingManager.getDonationStatusRenderer());
        adapter.setOnProductInteractionListener(this);
        recycler.setAdapter(adapter);

        mBillingManager.getDonationStatus().observe(getViewLifecycleOwner(), adapter::setDonationStatus);
        mBillingManager.getPurchasableProducts().observe(getViewLifecycleOwner(), adapter::setProducts);
    }

    @Override
    protected int layoutId() {
        return R.layout.fragment_donate;
    }

}
