package org.igarape.copcast.utils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by martelli on 12/9/15.
 */
public class GenericExtFilter implements FilenameFilter {

    private String ext;

    public GenericExtFilter(String ext) {
        this.ext = ext;
    }

    public boolean accept(File dir, String name) {
        return (name.endsWith(ext));
    }
}
