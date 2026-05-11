package com.example.socialonetwo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceFragment extends Fragment {

    public interface WorkspaceListener {
        void onApplyWorkspace(List<String> tabs);
        void onClearWorkspace();
    }

    private WorkspaceListener listener;
    private List<Workspace> workspaceList;
    private WorkspaceAdapter adapter;
    private List<String> currentOpenTabs;

    public void setListener(WorkspaceListener listener) {
        this.listener = listener;
    }

    public void setCurrentOpenTabs(List<String> tabs) {
        this.currentOpenTabs = tabs;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workspace, container, false);

        loadWorkspaces();

        RecyclerView rv = view.findViewById(R.id.workspaceRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new WorkspaceAdapter(workspaceList, new WorkspaceAdapter.OnWorkspaceActionListener() {
            @Override
            public void onLaunch(Workspace workspace) {
                if (listener != null) {
                    listener.onApplyWorkspace(workspace.tabs);
                }
            }

            @Override
            public void onEdit(int position, Workspace workspace) {
                showEditWorkspaceDialog(position, workspace);
            }

            @Override
            public void onDelete(int position) {
                showDeleteConfirmation(position);
            }
        });
        rv.setAdapter(adapter);

        View btnAdd = view.findViewById(R.id.btnAddWorkspace);
        UIUtils.setClickAnimation(getContext(), btnAdd);
        btnAdd.setOnClickListener(v -> showAddWorkspaceDialog());

        View btnClear = view.findViewById(R.id.btnClearWorkspace);
        UIUtils.setClickAnimation(getContext(), btnClear);
        btnClear.setOnClickListener(v -> {
            if (listener != null) listener.onClearWorkspace();
        });

        return view;
    }

    private void showAddWorkspaceDialog() {
        showWorkspaceDialog(-1, null);
    }

    private void showEditWorkspaceDialog(int position, Workspace workspace) {
        showWorkspaceDialog(position, workspace);
    }

    private void showWorkspaceDialog(int position, @Nullable Workspace existing) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_workspace, null);
        EditText inputName = dialogView.findViewById(R.id.editWorkspaceName);
        EditText inputTabs = dialogView.findViewById(R.id.editWorkspaceTabs);
        RecyclerView rvSelect = dialogView.findViewById(R.id.rvSelectTabs);

        if (existing != null) {
            inputName.setText(existing.name);
            StringBuilder sb = new StringBuilder();
            for (String t : existing.tabs) {
                sb.append(t).append("\n");
            }
            inputTabs.setText(sb.toString().trim());
        }

        List<String> selectable = new ArrayList<>();
        if (currentOpenTabs != null) {
            for (String t : currentOpenTabs) {
                if (t != null && t.contains(".") && !t.startsWith("home://") && !selectable.contains(t)) {
                    selectable.add(t);
                }
            }
        }

        SelectableTabsAdapter selectAdapter = new SelectableTabsAdapter(selectable);
        rvSelect.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvSelect.setAdapter(selectAdapter);
        rvSelect.setItemAnimator(null); // Disable animations to prevent scroll glitches
        rvSelect.post(() -> rvSelect.scrollToPosition(0)); // Ensure it starts at the beginning

        if (selectable.isEmpty()) {
            dialogView.findViewById(R.id.tvSelectTabsLabel).setVisibility(View.GONE);
            rvSelect.setVisibility(View.GONE);
        }

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(existing == null ? "New Workspace" : "Edit Workspace")
            .setView(dialogView)
            .setPositiveButton(existing == null ? "Create" : "Save", (dialog, which) -> {
                String name = inputName.getText().toString().trim();
                String tabsStr = inputTabs.getText().toString().trim();

                if (name.isEmpty()) {
                    Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> tabs = new ArrayList<>();
                
                // Add from selection
                tabs.addAll(selectAdapter.getSelectedTabs());

                // Add from text input
                String[] lines = tabsStr.split("\n");
                for (String line : lines) {
                    String url = line.trim();
                    if (!url.isEmpty()) {
                        if (!url.startsWith("http") && !url.contains("://")) {
                            url = "https://" + url;
                        }
                        if (!tabs.contains(url)) tabs.add(url);
                    }
                }

                if (tabs.isEmpty()) {
                    Toast.makeText(getContext(), "Add at least one URL", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (existing == null) {
                    workspaceList.add(new Workspace(name, tabs));
                    adapter.notifyItemInserted(workspaceList.size() - 1);
                } else {
                    existing.name = name;
                    existing.tabs = tabs;
                    adapter.notifyItemChanged(position);
                }
                saveWorkspaces();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showDeleteConfirmation(int position) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Workspace")
            .setMessage("Are you sure you want to delete this workspace?")
            .setPositiveButton("Delete", (dialog, which) -> {
                workspaceList.remove(position);
                saveWorkspaces();
                adapter.notifyItemRemoved(position);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loadWorkspaces() {
        workspaceList = new ArrayList<>();
        PreferenceManager pm = getPreferenceManager();
        if (pm == null) return;
        String json = pm.loadWorkspacesJson();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.getString("name");
                JSONArray tabsArray = obj.getJSONArray("tabs");
                List<String> tabs = new ArrayList<>();
                for (int j = 0; j < tabsArray.length(); j++) {
                    tabs.add(tabsArray.getString(j));
                }
                workspaceList.add(new Workspace(name, tabs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveWorkspaces() {
        PreferenceManager pm = getPreferenceManager();
        if (pm == null) return;
        try {
            JSONArray array = new JSONArray();
            for (Workspace w : workspaceList) {
                JSONObject obj = new JSONObject();
                obj.put("name", w.name);
                JSONArray tabsArray = new JSONArray();
                for (String t : w.tabs) {
                    tabsArray.put(t);
                }
                obj.put("tabs", tabsArray);
                array.put(obj);
            }
            pm.saveWorkspacesJson(array.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PreferenceManager getPreferenceManager() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).getPreferenceManager();
        }
        return null;
    }

    public static class Workspace {
        String name;
        List<String> tabs;

        Workspace(String name, List<String> tabs) {
            this.name = name;
            this.tabs = tabs;
        }
    }
}