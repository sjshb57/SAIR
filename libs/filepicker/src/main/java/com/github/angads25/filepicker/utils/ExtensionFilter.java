/*
 * Copyright (C) 2016 Angad Singh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.angads25.filepicker.utils;

import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

/**
 * <p>
 * Created by Angad Singh on 11-07-2016.
 * </p >
 */

/*  Class to filter the list of files.
 */
public class ExtensionFilter implements FileFilter {
    private final String[] validExtensions;
    private final DialogProperties properties;

    public ExtensionFilter(DialogProperties properties) {
        if (properties.extensions != null) {
            this.validExtensions = properties.extensions;
        } else {
            this.validExtensions = new String[]{""};
        }
        this.properties = properties;
    }

    /**
     * Function to filter files based on defined rules.
     */
    @Override
    public boolean accept(File file) {
        // Skip hidden files if configured to hide them
        if (!properties.showHiddenFiles && file.isHidden()) {
            return false;
        }

        // All readable directories are allowed
        if (file.isDirectory() && file.canRead()) {
            return true;
        }

        // Skip files if selection type is directory only
        if (properties.selection_type == DialogConfigs.DIR_SELECT) {
            return false;
        }

        // Check file extensions if specified
        if (validExtensions.length == 1 && validExtensions[0].isEmpty()) {
            return true;
        }

        String name = file.getName().toLowerCase(Locale.getDefault());
        for (String ext : validExtensions) {
            if (name.endsWith(ext.toLowerCase(Locale.getDefault()))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the properties associated with this filter
     */
    @SuppressWarnings("unused")
    public DialogProperties getProperties() {
        return properties;
    }
}