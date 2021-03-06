package com.duy.android.compiler.file.android;

import com.android.annotations.Nullable;
import com.android.ide.common.xml.AndroidManifestParser;
import com.android.ide.common.xml.ManifestData;
import com.duy.android.compiler.file.java.ClassFile;
import com.duy.android.compiler.file.java.JavaProject;
import com.google.common.base.MoreObjects;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

/**
 * Created by Duy on 05-Aug-17.
 */
public class AndroidProject extends JavaProject {

    private File xmlManifest;
    /* Output */
    private File outResourceFile;
    private File apkUnsigned;
    private File apkSigned;

    /* PROJECT */
    private ArrayList<File> resDirs;
    private ArrayList<File> assetsDirs;
    private File classR;

    private ManifestData.Activity launcherActivity;

    public AndroidProject(File dirRoot,
                          @Nullable String mainClassName,
                          @Nullable String packageName,
                          String projectName) {
        super(dirRoot, mainClassName, packageName, projectName);
    }

    @Override
    public void init() {
        super.init();

        resDirs = new ArrayList<>();
        resDirs.add(new File(dirSrcMain, "res"));

        assetsDirs = new ArrayList<>();
        assetsDirs.add(new File(dirSrcMain, "assets"));
        xmlManifest = new File(dirSrcMain, "AndroidManifest.xml");

        apkUnsigned = new File(dirBuildOutput, "app-unsigned-debug.apk");
        apkSigned = new File(dirBuildOutput, "app-debug.apk");

        createClassR();

        outResourceFile = new File(dirBuild, "resources.ap_");
    }

    @Nullable
    public ManifestData.Activity getLauncherActivity() {
        try {
            ManifestData manifestData = AndroidManifestParser.parse(new FileInputStream(getXmlManifest()));
            ManifestData.Activity launcherActivity = manifestData.getLauncherActivity();
            this.launcherActivity = launcherActivity;
            return launcherActivity;
        } catch (Exception e) {
            return null;
        }
    }


    public void createClassR() {
        if (packageName != null) {
            String path = packageName.replace(".", File.separator) + File.separator + "R.java";
            classR = new File(dirGeneratedSource, path);
        }
    }

    public File getXmlManifest() {
        return xmlManifest;
    }

    public File getApkSigned() {
        apkSigned.getParentFile().mkdirs();
        return apkSigned;
    }

    public File getOutResourceFile() {
        outResourceFile.getParentFile().mkdirs();
        return outResourceFile;
    }

    @Override
    public void clean() {
        super.clean();
        apkUnsigned.delete();
        apkSigned.delete();
    }

    @Override
    public void mkdirs() {
        super.mkdirs();
        getResDirs();
        getAssetsDirs();

        File resDir = resDirs.get(0);
        new File(resDir, "menu").mkdirs();
        new File(resDir, "layout").mkdirs();
        new File(resDir, "drawable").mkdirs();
        new File(resDir, "drawable-hdpi").mkdirs();
        new File(resDir, "drawable-xhdpi").mkdirs();
        new File(resDir, "drawable-xxhdpi").mkdirs();
        new File(resDir, "drawable-xxxhdpi").mkdirs();
        new File(resDir, "values").mkdirs();
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .toString();
    }

    @Override
    public String getSourcePath() {
        String sourcePath = super.getSourcePath();
        File generatedSource = new File(dirGenerated, "source");
        sourcePath += File.pathSeparator + generatedSource.getPath();
        return sourcePath;
    }

    public File getApkUnsigned() {
        apkUnsigned.getParentFile().mkdirs();
        return apkUnsigned;
    }

    public File getResDirs() {
        mkdirs(resDirs);
        return resDirs.get(0);
    }

    public File getAssetsDirs() {
        mkdirs(assetsDirs);
        return assetsDirs.get(0);
    }

    public File getClassR() {
        classR.getParentFile().mkdirs();
        return classR;
    }

    public File getDirLayout() {
        File file = new File(getResDirs(), "layout");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    /**
     * use for javac
     *
     * @return main class
     */
    @Override
    public ClassFile getMainClass() {
        if (launcherActivity == null) getLauncherActivity();
        if (launcherActivity != null) {
            return new ClassFile(this.launcherActivity.getName());
        } else return null;
    }

}
