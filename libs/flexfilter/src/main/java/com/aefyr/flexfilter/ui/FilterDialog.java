package com.aefyr.flexfilter.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.flexfilter.config.core.ComplexFilterConfig;
import com.aefyr.flexfilter.ui.adapter.ComplexFilterConfigAdapter;
import com.aefyr.flexfilter.ui.adapter.FilterConfigViewHolderFactory;
import com.aefyr.flexfilter.ui.viewmodel.FilterConfigViewModel;
import com.aefyr.flexfilter.ui.viewmodel.FilterConfigViewModelFactory;

import java.util.Objects;

public class FilterDialog extends BaseBottomSheetDialogFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_FILTERING_CONFIG = "filtering_config";
    private static final String ARG_VH_FACTORY = "vh_factory";

    private CharSequence mTitle;
    private FilterConfigViewModel mViewModel;
    private FilterConfigViewHolderFactory mVhFactory;

    private RecyclerView mRecycler;

    public static FilterDialog newInstance(CharSequence title, ComplexFilterConfig config, Class<? extends FilterConfigViewHolderFactory> vhFactoryClass) {
        Bundle args = new Bundle();
        args.putCharSequence(ARG_TITLE, title);
        args.putParcelable(ARG_FILTERING_CONFIG, config.snapshot());
        args.putString(ARG_VH_FACTORY, vhFactoryClass.getCanonicalName());

        FilterDialog filterDialog = new FilterDialog();
        filterDialog.setArguments(args);
        return filterDialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null)
            throw new NullPointerException("No arguments passed");

        mTitle = args.getCharSequence(ARG_TITLE);
        ComplexFilterConfig config = args.getParcelable(ARG_FILTERING_CONFIG);
        try {
            mVhFactory = (FilterConfigViewHolderFactory) Class.forName(Objects.requireNonNull(args.getString(ARG_VH_FACTORY))).getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mViewModel = new ViewModelProvider(this, new FilterConfigViewModelFactory(config)).get(FilterConfigViewModel.class);
    }

    @Nullable
    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = new RecyclerView(requireContext());
        recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        return recyclerView;
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mRecycler = (RecyclerView) view;

        setTitle(mTitle);

        getNegativeButton().setOnClickListener(v -> dismiss());
        getPositiveButton().setOnClickListener(v -> {
            try {
                OnApplyConfigListener listener;
                if (getParentFragment() != null)
                    listener = (OnApplyConfigListener) getParentFragment();
                else
                    listener = (OnApplyConfigListener) getActivity();

                if (listener != null)
                    listener.onApplyConfig(mViewModel.getConfig());
            } catch (Exception e) {
                throw new IllegalStateException("Activity/Fragment that uses FilterDialog must implement FilterDialog.OnApplyConfigListener");
            }
            dismiss();
        });

        bindViewModel();
        getPositiveButton().requestFocus();
    }

    private void bindViewModel() {

        ComplexFilterConfigAdapter adapter = new ComplexFilterConfigAdapter(mVhFactory);
        mRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecycler.setAdapter(adapter);
        mViewModel.getFilters().observe(this, (filters) -> {
            adapter.setFilters(filters);
            revealBottomSheet();
        });
    }

    public interface OnApplyConfigListener {

        void onApplyConfig(ComplexFilterConfig config);

    }

}
