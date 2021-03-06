/*
 *  Copyright (c) 2017 Tran Le Duy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duy.ide.editor.code;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.duy.JavaApplication;
import com.duy.android.compiler.BuildJar;
import com.duy.android.compiler.BuildTask;
import com.duy.android.compiler.builder.AndroidProjectBuilder;
import com.duy.android.compiler.builder.IBuilder;
import com.duy.android.compiler.builder.JavaProjectBuilder;
import com.duy.android.compiler.file.android.AndroidProject;
import com.duy.android.compiler.file.java.ClassFile;
import com.duy.android.compiler.file.java.ClassUtil;
import com.duy.android.compiler.file.java.JavaProject;
import com.duy.ide.Builder;
import com.duy.ide.CompileManager;
import com.duy.ide.MenuEditor;
import com.duy.ide.R;
import com.duy.ide.diagnostic.DiagnosticFragment;
import com.duy.ide.editor.code.view.EditorView;
import com.duy.ide.editor.code.view.IndentEditText;
import com.duy.ide.editor.uidesigner.inflate.DialogLayoutPreview;
import com.duy.ide.javaide.autocomplete.AutoCompleteProvider;
import com.duy.ide.javaide.autocomplete.model.Description;
import com.duy.ide.javaide.autocomplete.util.JavaUtil;
import com.duy.ide.javaide.sample.activities.DocumentActivity;
import com.duy.ide.javaide.sample.activities.SampleActivity;
import com.duy.ide.setting.AppSetting;
import com.duy.ide.utils.RootUtils;
import com.duy.projectview.ProjectManager;
import com.duy.ide.javaide.run.activities.ExecuteActivity;
import com.duy.ide.javaide.run.dialog.DialogRunConfig;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;

public class MainActivity extends ProjectManagerActivity implements
        DrawerLayout.DrawerListener,
        DialogRunConfig.OnConfigChangeListener,
        Builder {
    public static final int REQUEST_CODE_SAMPLE = 1015;

    private static final String TAG = "MainActivity";

    private CompileManager mCompileManager;
    private MenuEditor mMenuEditor;
    private Dialog mDialog;
    private MenuItem mActionRun;
    private ProgressBar mCompileProgress;
    private AutoCompleteProvider mAutoCompleteProvider;

    private void populateAutoCompleteService(AutoCompleteProvider provider) {
        mPagePresenter.setAutoCompleteProvider(provider);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCompileManager = new CompileManager(this);
        mMenuEditor = new MenuEditor(this, this);
        initView(savedInstanceState);
        startAutoCompleteService();
    }

    protected void startAutoCompleteService() {
        Log.d(TAG, "startAutoCompleteService() called");
        if (mAutoCompleteProvider == null) {
            if (mProject != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mAutoCompleteProvider = new AutoCompleteProvider(MainActivity.this);
                        mAutoCompleteProvider.load(mProject);
                        populateAutoCompleteService(mAutoCompleteProvider);
                    }
                }).start();
            }
        }
    }


    public void initView(Bundle savedInstanceState) {
        mDrawerLayout.addDrawerListener(this);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                mDrawerLayout.closeDrawers();
                return mMenuEditor.onOptionsItemSelected(item);
            }
        });
        View tab = findViewById(R.id.img_tab);
        if (tab != null) {
            tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    insertTab(v);
                }
            });
        }
        mCompileProgress = findViewById(R.id.compile_progress);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mMenuEditor.onOptionsItemSelected(item);
    }

    @Override
    public void invalidateOptionsMenu() {
        super.invalidateOptionsMenu();
    }

    void insertTab(View v) {
        onKeyClick(v, IndentEditText.TAB_CHARACTER);
    }

    @Override
    public void onKeyClick(View view, String text) {
        EditorFragment currentFragment = mPageAdapter.getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.insert(text);
        }
    }

    @Override
    public void onKeyLongClick(String text) {
        EditorFragment currentFragment = mPageAdapter.getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.insert(text);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean r = mMenuEditor.onCreateOptionsMenu(menu);
        mActionRun = menu.findItem(R.id.action_edit_run);
        return r;
    }

    /**
     * create dialog find and replace
     */
    @Override
    public void findAndReplace() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(R.layout.dialog_find_and_replace);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        final CheckBox ckbRegex = alertDialog.findViewById(R.id.ckb_regex);
        final CheckBox ckbMatch = alertDialog.findViewById(R.id.ckb_match_key);
        final EditText editFind = alertDialog.findViewById(R.id.txt_find);
        final EditText editReplace = alertDialog.findViewById(R.id.edit_replace);
        if (editFind != null) {
            editFind.setText(getPreferences().getString(AppSetting.LAST_FIND));
        }
        View find = alertDialog.findViewById(R.id.btn_replace);
        assert find != null;
        assert editFind != null;
        assert editReplace != null;
        assert ckbRegex != null;
        assert ckbMatch != null;
        find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
                if (editorFragment != null) {

                    editorFragment.doFindAndReplace(
                            editFind.getText().toString(),
                            editReplace.getText().toString(),
                            ckbRegex.isChecked(),
                            ckbMatch.isChecked());
                }
                getPreferences().put(AppSetting.LAST_FIND, editFind.getText().toString());
                alertDialog.dismiss();
            }
        });
        View cancle = alertDialog.findViewById(R.id.btn_cancel);
        assert cancle != null;
        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
    }

    @Override
    public void runProject() {
        saveAllFile();
        if (mProject != null) {
            if (mProject instanceof AndroidProject) {
                compileAndroidProject();
            } else {
                compileJavaProject();
            }
        } else {
            Toast.makeText(this, "You need create project", Toast.LENGTH_SHORT).show();
        }
    }

    private void compileAndroidProject() {
        if (mProject instanceof AndroidProject) {
            if (!((AndroidProject) mProject).getXmlManifest().exists()) {
                Toast.makeText(this, "Can not find AndroidManifest.xml", Toast.LENGTH_SHORT).show();
                return;
            }
            //check launcher activity
            if (((AndroidProject) mProject).getLauncherActivity() == null) {
                String msg = getString(R.string.can_not_find_launcher_activity);
                Snackbar.make(findViewById(R.id.coordinate_layout), msg, Snackbar.LENGTH_LONG)
                        .setAction(R.string.config, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                            }
                        }).show();
                return;
            }


            final DiagnosticCollector mDiagnosticCollector = new DiagnosticCollector();
            final IBuilder<AndroidProject> builder = new AndroidProjectBuilder(this, (AndroidProject) mProject, mDiagnosticCollector);
            final BuildTask<AndroidProject> buildTask = new BuildTask<>(builder, new BuildTask.CompileListener<AndroidProject>() {
                @Override
                public void onStart() {
                    updateUiStartCompile();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(MainActivity.this, R.string.failed_msg, Toast.LENGTH_SHORT).show();
                    openDrawer(GravityCompat.START);
                    mDiagnosticPresenter.display(mDiagnosticCollector.getDiagnostics());
                    updateUIFinish();
                }

                @Override
                public void onComplete() {
                    updateUIFinish();
                    Toast.makeText(MainActivity.this, R.string.build_success, Toast.LENGTH_SHORT).show();
                    mFilePresenter.refresh(mProject);
                    mDiagnosticPresenter.display(mDiagnosticCollector.getDiagnostics());
                    mContainerOutput.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    RootUtils.installApk(MainActivity.this, ((AndroidProject) mProject).getApkSigned());
                }

            });
            buildTask.execute();
        } else {
            if (mProject != null) {
                toast("This is Java project, please create new Android project");
            } else {
                toast("You need create project");
            }
        }
    }


    private void compileJavaProject() {
        //check main class exist
        if (mProject.getMainClass() == null
                || mProject.getPackageName() == null
                || mProject.getPackageName().isEmpty()
                || !mProject.getMainClass().exist(mProject)) {
            String msg = getString(R.string.main_class_not_define);
            Snackbar.make(findViewById(R.id.coordinate_layout), msg, Snackbar.LENGTH_LONG)
                    .setAction(R.string.select, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showDialogRunConfig();
                        }
                    }).show();
            return;
        }
        //check main function exist
        if (!ClassUtil.hasMainFunction(new File(mProject.getMainClass().getPath(mProject)))) {
            SpannableStringBuilder msg = new SpannableStringBuilder(getString(R.string.can_not_find_main_func));
            Spannable clasz = new SpannableString(mProject.getMainClass().getName());
            clasz.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.dark_color_accent))
                    , 0, clasz.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            msg.append(clasz);
            Snackbar.make(findViewById(R.id.coordinate_layout), msg, Snackbar.LENGTH_LONG)
                    .setAction(R.string.config, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showDialogRunConfig();
                        }
                    }).show();
            return;
        }

        final DiagnosticCollector mDiagnosticCollector = new DiagnosticCollector();
        final IBuilder<JavaProject> builder = new JavaProjectBuilder(this, mProject, mDiagnosticCollector);
        final BuildTask.CompileListener<JavaProject> listener = new BuildTask.CompileListener<JavaProject>() {
            @Override
            public void onStart() {
                updateUiStartCompile();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, R.string.failed_msg, Toast.LENGTH_SHORT).show();
                openDrawer(GravityCompat.START);
                mBottomPage.setCurrentItem(DiagnosticFragment.INDEX);
                mDiagnosticPresenter.display(mDiagnosticCollector.getDiagnostics());
                updateUIFinish();
            }

            @Override
            public void onComplete() {
                updateUIFinish();
                Toast.makeText(MainActivity.this, R.string.compile_success, Toast.LENGTH_SHORT).show();
                mDiagnosticPresenter.display(mDiagnosticCollector.getDiagnostics());
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, ExecuteActivity.class);
                        intent.putExtra(ExecuteActivity.PROJECT_FILE, mProject);
                        startActivity(intent);

                    }
                }, 200);
            }
        };
        BuildTask<JavaProject> buildTask = new BuildTask<>(builder, listener);
        buildTask.execute();
    }

    @Override
    public void buildJar() {
        saveAllFile();
        if (mProject != null) {
            new BuildJar(this, new BuildJar.CompileListener() {
                @Override
                public void onStart() {
                    updateUiStartCompile();
                }

                @Override
                public void onError(Exception e, List<Diagnostic> diagnostics) {
                    Toast.makeText(MainActivity.this, R.string.failed_msg, Toast.LENGTH_SHORT).show();
                    openDrawer(GravityCompat.START);
                    mBottomPage.setCurrentItem(DiagnosticFragment.INDEX);
                    mDiagnosticPresenter.display(diagnostics);
                    mContainerOutput.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                    updateUIFinish();
                }

                @Override
                public void onComplete(File jarfile, List<Diagnostic> diagnostics) {
                    Toast.makeText(MainActivity.this, R.string.build_success + " " + jarfile.getPath(),
                            Toast.LENGTH_SHORT).show();
                    mFilePresenter.refresh(mProject);
                    mDiagnosticPresenter.display(diagnostics);
                    mContainerOutput.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    updateUIFinish();
                }

            }).execute(mProject);
        } else {
            toast("You need create project");
        }
    }

    public void buildApk() {
        compileAndroidProject();
    }

    /**
     * replace dialog find
     */
    public void showDialogFind() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(R.layout.dialog_find);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        final CheckBox ckbRegex = alertDialog.findViewById(R.id.ckb_regex);
        final CheckBox ckbMatch = alertDialog.findViewById(R.id.ckb_match_key);
        final CheckBox ckbWordOnly = alertDialog.findViewById(R.id.ckb_word_only);
        final EditText editFind = alertDialog.findViewById(R.id.txt_find);
        editFind.setText(getPreferences().getString(AppSetting.LAST_FIND));
        alertDialog.findViewById(R.id.btn_replace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
                if (editorFragment != null) {
                    editorFragment.doFind(editFind.getText().toString(),
                            ckbRegex.isChecked(),
                            ckbWordOnly.isChecked(),
                            ckbMatch.isChecked());
                }
                getPreferences().put(AppSetting.LAST_FIND, editFind.getText().toString());
                alertDialog.dismiss();
            }
        });
        alertDialog.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });


    }

    @Override
    public void saveCurrentFile() {
        EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
        if (editorFragment != null) {
            editorFragment.saveFile();
        }
    }

    @Override
    public void showDocumentActivity() {
        Intent intent = new Intent(this, DocumentActivity.class);
        startActivity(intent);
    }

    public String getCode() {
        EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
        if (editorFragment != null) {
            return editorFragment.getCode();
        }
        return "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getPreferences().isShowListSymbol()) {
            if (mContainerSymbol != null && mKeyList != null) {
                mKeyList.setListener(this);
                mContainerSymbol.setVisibility(View.VISIBLE);
            }
        } else {
            if (mContainerSymbol != null) {
                mContainerSymbol.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(@NonNull SharedPreferences sharedPreferences, @NonNull String s) {
        if (s.equals(getString(R.string.key_show_suggest_popup))
                || s.equals(getString(R.string.key_show_line_number))
                || s.equals(getString(R.string.key_pref_word_wrap))) {
            EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
            if (editorFragment != null) {
                editorFragment.refreshCodeEditor();
            }
        } else if (s.equals(getString(R.string.key_show_symbol))) {
            if (mContainerSymbol != null) {
                mContainerSymbol.setVisibility(getPreferences().isShowListSymbol()
                        ? View.VISIBLE : View.GONE);
            }
        } else if (s.equals(getString(R.string.key_show_suggest_popup))) {
            EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
            if (editorFragment != null) {
                EditorView editor = editorFragment.getEditor();
                editor.setSuggestData(new ArrayList<Description>());
            }
        }
        //toggle ime/no suggest mode
        else if (s.equalsIgnoreCase(getString(R.string.key_ime_keyboard))) {
            EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
            if (editorFragment != null) {
                EditorView editor = editorFragment.getEditor();
                editorFragment.refreshCodeEditor();
            }
        } else {
            super.onSharedPreferenceChanged(sharedPreferences, s);
        }
    }

    /**
     * show dialog create new source file
     */
    @Override
    public void createNewFile(View view) {
//        DialogCreateNewFile dialogCreateNewFile = DialogCreateNewFile.Companion.getInstance();
//        dialogCreateNewFile.show(getSupportFragmentManager(), DialogCreateNewFile.Companion.getTAG());
//        dialogCreateNewFile.setListener(new DialogCreateNewFile.OnCreateNewFileListener() {
//            @Override
//            public void onFileCreated(@NonNull File file) {
//                saveFile();
//                //add to view
//                addNewPageEditor(file, SELECT);
//                mDrawerLayout.closeDrawers();
//            }
//
//            @Override
//            public void onCancel() {
//            }
//        });
    }

    @Override
    public void goToLine() {
        final AppCompatEditText edittext = new AppCompatEditText(this);
        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);
        edittext.setMaxEms(5);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.goto_line)
                .setView(edittext)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String line = edittext.getText().toString();
                        if (!line.isEmpty()) {
                            EditorFragment editorFragment
                                    = mPageAdapter.getCurrentFragment();
                            if (editorFragment != null) {
                                editorFragment.goToLine(Integer.parseInt(line));
                            }
                        }
                        dialog.cancel();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    @Override
    public void formatCode() {
        EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
        if (editorFragment != null) {
            editorFragment.formatCode();
        }
    }

    @Override
    public void undo() {
        EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
        if (editorFragment != null) {
            editorFragment.undo();
        }
    }

    @Override
    public void redo() {
        EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
        if (editorFragment != null) {
            editorFragment.redo();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SAMPLE:
                if (resultCode == RESULT_OK) {
                    final JavaProject projectFile = (JavaProject)
                            data.getSerializableExtra(SampleActivity.PROJECT_FILE);
                    if (projectFile != null) {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                onProjectCreated(projectFile);
                            }
                        }, 100);
                    }
                }
                break;
        }
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(View drawerView) {
        closeKeyBoard();
    }

    @Override
    public void onDrawerClosed(View drawerView) {

    }

    @Override
    public void paste() {
        EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
        if (editorFragment != null) {
            editorFragment.paste();
        }
    }

    @Override
    public void copyAll() {
        EditorFragment editorFragment = mPageAdapter.getCurrentFragment();
        if (editorFragment != null) {
            editorFragment.copyAll();
        }
    }

    @Override
    public void selectThemeFont() {
    }

    @Override
    public void runFile(String filePath) {
        saveCurrentFile();
        if (mProject == null) return;
        boolean canRun = ClassUtil.hasMainFunction(new File(filePath));
        if (!canRun) {
            Toast.makeText(this, (getString(R.string.main_not_found)), Toast.LENGTH_SHORT).show();
            return;
        }
        String className = JavaUtil.getClassName(mProject.getJavaSrcDirs().get(0), filePath);
        if (className == null) {
            Toast.makeText(this, ("Class \"" + filePath + "\"" + "invalid"), Toast.LENGTH_SHORT).show();
            return;
        }
        mProject.setMainClass(new ClassFile(className));
        runProject();
    }

    @Override
    public void previewLayout(String path) {
        saveCurrentFile();
        File currentFile = getCurrentFile();
        if (currentFile != null) {
            DialogLayoutPreview dialogPreview = DialogLayoutPreview.newInstance(currentFile);
            dialogPreview.show(getSupportFragmentManager(), DialogLayoutPreview.TAG);
        } else {
            Toast.makeText(this, "Can not find file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void createKeyStore() {
        // TODO: 22-Aug-17 support this feature
//        Intent intent = new Intent(this, CreateKeyStoreActivity.class);
//        intent.putExtra("project_path", mProjectFile.getProjectDir());
    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)
                || mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            if (mContainerOutput.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                mContainerOutput.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                return;
            } else {
                mDrawerLayout.closeDrawers();
                return;
            }
        }

//        /*
//          check can undo
//         */
//        if (getPreferences().getBoolean(getString(R.string.key_back_undo))) {
//            undo();
//            return;
//        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.exit)
                .setMessage(R.string.exit_mgs)
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create().show();
    }

    public void showDialogRunConfig() {
        if (mProject != null) {
            DialogRunConfig dialogRunConfig = DialogRunConfig.newInstance(mProject);
            dialogRunConfig.show(getSupportFragmentManager(), DialogRunConfig.TAG);
        } else {
            Toast.makeText(this, "Please create project", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConfigChange(JavaProject projectFile) {
        this.mProject = projectFile;
        if (projectFile != null) {
            ProjectManager.saveProject(this, projectFile);
        }
    }


    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void updateUiStartCompile() {
        if (mActionRun != null) mActionRun.setEnabled(false);
        if (mCompileProgress != null) mCompileProgress.setVisibility(View.VISIBLE);
        hideKeyboard();
        openDrawer(GravityCompat.START);

        mMessagePresenter.resume((JavaApplication) getApplication());
        mMessagePresenter.clear();
        mMessagePresenter.append("Compiling...\n");

        mContainerOutput.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        mDiagnosticPresenter.clear();

        mBottomPage.setCurrentItem(0);

    }

    private void updateUIFinish() {
        mMessagePresenter.pause((JavaApplication) getApplication());

        if (mActionRun != null) mActionRun.setEnabled(true);
        if (mCompileProgress != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCompileProgress.setVisibility(View.GONE);
                }
            }, 500);
        }
    }


}