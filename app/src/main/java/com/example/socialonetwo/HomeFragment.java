package com.example.socialonetwo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HomeFragment extends Fragment {

    private List<String> historyList;
    private List<String> bookmarksList;
    private RecentSitesAdapter.OnRecentClickListener listener;
    private View.OnClickListener appInfoListener;
    private View.OnClickListener quickAccessListener;
    private View.OnClickListener workspaceListener;

    private RecentSitesAdapter recentSitesAdapter;
    private RecentSitesAdapter bookmarksAdapter;
    private TextView recentPlaceholder, bookmarksPlaceholder;

    public static HomeFragment newInstance(List<String> historyList, List<String> bookmarksList) {
        HomeFragment fragment = new HomeFragment();
        fragment.historyList = historyList;
        fragment.bookmarksList = bookmarksList;
        return fragment;
    }

    public void setData(List<String> historyList, List<String> bookmarksList) {
        this.historyList = historyList;
        this.bookmarksList = bookmarksList;
    }

    public void setListeners(RecentSitesAdapter.OnRecentClickListener listener, 
                             View.OnClickListener appInfoListener,
                             View.OnClickListener quickAccessListener,
                             View.OnClickListener workspaceListener) {
        this.listener = listener;
        this.appInfoListener = appInfoListener;
        this.quickAccessListener = quickAccessListener;
        this.workspaceListener = workspaceListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        View btnAppInfo = view.findViewById(R.id.btnAppInfo);
        if (appInfoListener != null) btnAppInfo.setOnClickListener(appInfoListener);
        UIUtils.setClickAnimation(getContext(), btnAppInfo);

        View btnOpenQuickAccess = view.findViewById(R.id.btnOpenQuickAccessMessages);
        if (quickAccessListener != null) btnOpenQuickAccess.setOnClickListener(quickAccessListener);
        UIUtils.setClickAnimation(getContext(), btnOpenQuickAccess);

        View btnOpenWorkspace = view.findViewById(R.id.btnOpenWorkspace);
        if (workspaceListener != null) btnOpenWorkspace.setOnClickListener(workspaceListener);
        UIUtils.setClickAnimation(getContext(), btnOpenWorkspace);

        RecyclerView recentRecyclerView = view.findViewById(R.id.recentRecyclerView);
        recentSitesAdapter = new RecentSitesAdapter(historyList, 8, listener);
        recentRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), getResources().getInteger(R.integer.dashboard_span_count)));
        recentRecyclerView.setAdapter(recentSitesAdapter);
        recentPlaceholder = view.findViewById(R.id.recentPlaceholder);

        RecyclerView bookmarksRecyclerView = view.findViewById(R.id.bookmarksRecyclerView);
        bookmarksAdapter = new RecentSitesAdapter(bookmarksList, -1, listener);
        bookmarksAdapter.setFilterUniqueDomains(false);
        bookmarksRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), getResources().getInteger(R.integer.dashboard_span_count)));
        bookmarksRecyclerView.setAdapter(bookmarksAdapter);
        bookmarksPlaceholder = view.findViewById(R.id.bookmarksPlaceholder);

        updateRecentVisibility();
        updateBookmarksVisibility();

        return view;
    }

    public void updateRecentVisibility() {
        if (recentPlaceholder != null) {
            recentPlaceholder.setVisibility((historyList == null || historyList.isEmpty()) ? View.VISIBLE : View.GONE);
        }
    }

    public void updateBookmarksVisibility() {
        if (bookmarksPlaceholder != null) {
            bookmarksPlaceholder.setVisibility((bookmarksList == null || bookmarksList.isEmpty()) ? View.VISIBLE : View.GONE);
        }
    }

    public void notifyDataChanged() {
        if (recentSitesAdapter != null) recentSitesAdapter.updateFilteredList();
        if (bookmarksAdapter != null) bookmarksAdapter.updateFilteredList();
        updateRecentVisibility();
        updateBookmarksVisibility();
    }
}
