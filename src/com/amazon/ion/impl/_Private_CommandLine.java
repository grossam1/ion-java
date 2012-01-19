// Copyright (c) 2010-2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.ion.impl;

import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonTextWriterBuilder;
import com.amazon.ion.util.JarInfo;
import java.io.IOException;

public final class _Private_CommandLine
{
    static final int argid_VERSION = 2;
    static final int argid_HELP    = 3;
    static final int argid_INVALID = -1;

    static boolean printHelp    = false;
    static boolean printVersion = false;
    static String  errorMessage = null;


    /**
     * This main simply prints the version information to
     * allow users to identify the build version of the Jar.
     *
     * @param args user command line flags
     */
    public static void main(String[] args) throws IOException
    {
        process_command_line(args);

        if (printVersion) {
            doPrintVersion();
        }
        if (printHelp) {
            doPrintHelp();
        }
    }

    private static void process_command_line(String[] args)
    {
        if (args.length == 0) printHelp = true;

        for (int ii=0; ii<args.length; ii++) {
            String arg = args[ii];
            if (arg == null || arg.length() < 1) continue;
            int argid = getArgumentId(arg);
            switch (argid) {
            case argid_VERSION:
                printVersion = true;
                break;
            case argid_HELP:
                printHelp = true;
                break;
            default:
                invalid_arg(ii, arg);
                break;
            }
        }
        return;
    }
    private static int getArgumentId(String arg)
    {
        if (arg.startsWith("-") && arg.length() == 2) {
            switch (arg.charAt(1)) {
            case 'h': case '?':
                return argid_HELP;
            case 'v':
                return argid_VERSION;
            default:
                return argid_INVALID;
            }
        }
        if (arg.startsWith("--") && arg.length() > 2) {
            if (arg.equals("help")) {
                return argid_HELP;
            }
            if (arg.equals("version")) {
                return argid_VERSION;
            }
        }
        return argid_INVALID;
    }
    private static void invalid_arg(int ii, String arg)
    {
        errorMessage += "\narg["+ii+"] \""+arg+"\" is unrecognized or invalid.";
        printHelp = true;
    }

    private static void doPrintHelp() {
        System.out.println("IonJava -- Copyright (c) 2010-2011 Amazon.com");
        System.out.println("usage: java -jar IonJava.jar <options>");
        System.out.println("options:");
        System.out.println("-v (--version) prints current version entry");
        System.out.println("-h (--help)    prints this helpful message");
        if (errorMessage != null) {
            System.out.println();
            System.out.println(errorMessage);
        }
    }


    private static void doPrintVersion() throws IOException
    {
        IonTextWriterBuilder b = IonTextWriterBuilder.pretty();
        b.setCharset(IonTextWriterBuilder.ASCII);

        JarInfo info = new JarInfo();

        IonWriter w = b.build((Appendable)System.out);
        w.stepIn(IonType.STRUCT);
        {
            w.setFieldName("release_label");
            w.writeString(info.getReleaseLabel());

            w.setFieldName("brazil_major_version");
            w.writeString(info.getBrazilMajorVersion());

            w.setFieldName("brazil_package_version");
            w.writeString(info.getBrazilPackageVersion());

            w.setFieldName("build_time");
            w.writeTimestamp(info.getBuildTime());
        }
        w.stepOut();
        w.finish();
        System.out.println();
    }
}