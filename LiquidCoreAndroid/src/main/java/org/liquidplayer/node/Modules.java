/*
 * Copyright (c) 2016 - 2018 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/LiquidCore for terms and conditions.
 */
package org.liquidplayer.node;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Modules {
    private final Context context;
    private static boolean modules_loaded = false;
    private static final Object mutex = new Object();

    Modules(Context ctx) {
        context = ctx;
        SQLite3Shim.init();
    }

    void setUpNodeModules() {
        synchronized (mutex) {
            File path = context.getFilesDir();
            String dest_dir_path = path + addLeadingSlash("__org.liquidplayer.node__/node_modules");
            File dest_dir = new File(dest_dir_path);

            if (!modules_loaded) {
                File version = new File(dest_dir_path + addLeadingSlash(".__version"));
                try {
                    boolean reload = true;
                    if (version.exists()) {
                        BufferedReader br = new BufferedReader(new FileReader(version));
                        String installed_version = br.readLine().trim();
                        br.close();
                        reload = !BuildConfig.VERSION_NAME.equals(installed_version);
                    }
                    if (reload) {
                        if (dest_dir.exists()) {
                            FileSystem.deleteRecursive(dest_dir);
                        }
                        copyDirorfileFromAssetManager("node_modules", "node_modules");
                        FileWriter writer = new FileWriter(dest_dir_path + addLeadingSlash(".__version"));
                        writer.write(BuildConfig.VERSION_NAME);
                        writer.close();
                    }
                    modules_loaded = true;
                } catch (IOException e) {
                    Log.e("setUpNodeModules", "Exception", e);
                }
            }
        }
    }

    private void copyDirorfileFromAssetManager(String arg_assetDir, String arg_destinationDir)
            throws IOException
    {
        File sd_path = context.getFilesDir();
        String dest_dir_path = sd_path + addLeadingSlash("__org.liquidplayer.node__") +
            addLeadingSlash(arg_destinationDir);
        File dest_dir = new File(dest_dir_path);

        createDir(dest_dir);

        AssetManager asset_manager = context.getAssets();
        String[] files = asset_manager.list(arg_assetDir);

        if (files == null) return;

        for (String file: files)
        {

            String abs_asset_file_path = addTrailingSlash(arg_assetDir) + file;
            String sub_files[] = asset_manager.list(abs_asset_file_path);

            if (sub_files == null || sub_files.length == 0)
            {
                // It is a file
                String dest_file_path = addTrailingSlash(dest_dir_path) + file;
                copyAssetFile(abs_asset_file_path, dest_file_path);
            } else
            {
                // It is a sub directory
                copyDirorfileFromAssetManager(abs_asset_file_path,
                        addTrailingSlash(arg_destinationDir) + file);
            }
        }
    }


    private void copyAssetFile(String assetFilePath, String destinationFilePath) throws IOException
    {
        Log.i("tag", "Copying " + assetFilePath + " to " + destinationFilePath);
        InputStream in = context.getAssets().open(assetFilePath);
        OutputStream out = new FileOutputStream(destinationFilePath);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);
        in.close();
        out.close();
    }

    private String addTrailingSlash(String path)
    {
        if (path.charAt(path.length() - 1) != '/')
        {
            path += "/";
        }
        return path;
    }

    private String addLeadingSlash(String path)
    {
        if (path.charAt(0) != '/')
        {
            path = "/" + path;
        }
        return path;
    }

    private void createDir(File dir) throws IOException
    {
        Log.i("tag", "Create dir " + dir);
        if (dir.exists())
        {
            if (!dir.isDirectory())
            {
                throw new IOException("Can't create directory, a file is in the way");
            }
        } else
        {
            if (dir.mkdirs()) {
                android.util.Log.i("LiquidCore", "Created directory " + dir);
            }
            if (!dir.isDirectory())
            {
                throw new IOException("Unable to create directory");
            }
        }
    }
}
