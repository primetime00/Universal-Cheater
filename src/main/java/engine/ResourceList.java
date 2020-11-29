package engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ResourceList {
    /**
     * for all elements of java.class.path get a Collection of resources Pattern
     * pattern = Pattern.compile(".*"); gets all resources
     *
     * @param pattern
     *            the pattern to match
     * @return the resources in the order they are found
     */
    public static Collection<String> getResources(
            final Pattern pattern){
        final ArrayList<String> retval = new ArrayList<String>();
        final String classPath = System.getProperty("java.class.path", ".");
        final String[] classPathElements = classPath.split(System.getProperty("path.separator"));
        for(final String element : classPathElements){
            retval.addAll(getResources(element, pattern));
        }
        return retval;
    }

    private static Collection<String> getResources(
            final String element,
            final Pattern pattern){
        final ArrayList<String> retval = new ArrayList<String>();
        final File file = new File(element);
        if(file.isDirectory()){
            retval.addAll(getResourcesFromDirectory(file, pattern));
        } else{
            retval.addAll(getResourcesFromJarFile(file, pattern));
        }
        return retval;
    }

    private static Collection<String> getResourcesFromJarFile(
            final File file,
            final Pattern pattern){
        final ArrayList<String> retval = new ArrayList<String>();
        ZipFile zf;
        try{
            zf = new ZipFile(file);
        } catch(final ZipException e){
            throw new Error(e);
        } catch(final IOException e){
            throw new Error(e);
        }
        final Enumeration e = zf.entries();
        while(e.hasMoreElements()){
            final ZipEntry ze = (ZipEntry) e.nextElement();
            final String fileName = ze.getName();
            final boolean accept = pattern.matcher(fileName).matches();
            if(accept){
                retval.add(fileName);
            }
        }
        try{
            zf.close();
        } catch(final IOException e1){
            throw new Error(e1);
        }
        return retval;
    }

    private static Collection<String> getResourcesFromDirectory(
            final File directory,
            final Pattern pattern){
        final ArrayList<String> retval = new ArrayList<String>();
        final File[] fileList = directory.listFiles();
        for(final File file : fileList){
            if(file.isDirectory()){
                retval.addAll(getResourcesFromDirectory(file, pattern));
            } else{
                try{
                    final String fileName = file.getCanonicalPath();
                    final boolean accept = pattern.matcher(fileName).matches();
                    if(accept){
                        retval.add(fileName);
                    }
                } catch(final IOException e){
                    throw new Error(e);
                }
            }
        }
        return retval;
    }

    public static Collection<ResourceAndDirectory> getResourcesAndDirectories(String root) {
        ArrayList<ResourceAndDirectory> rl = new ArrayList<>();

        getResources(Pattern.compile(String.format(".*%s.*", root))).forEach(path -> {
            String dir = path.substring(path.indexOf(root)+root.length()+1);
            if (dir.isEmpty())
                return;
            if (!dir.contains("."))
                return;
            rl.add(new ResourceAndDirectory(Path.of(dir).getParent().toString(), Path.of(dir).getFileName().toString()));
        });
        return rl;
    }

    public static Collection<String> getDistinctDirectories(Collection<ResourceAndDirectory> rl) {
        return rl.stream().map(e -> e.getDirectory()).distinct().filter(f->!f.contains("\\")).collect(Collectors.toList());
    }

    public static class ResourceAndDirectory {
        String directory;
        String file;

        public ResourceAndDirectory(String directory, String file) {
            this.directory = directory;
            this.file = file;
        }

        public String getDirectory() {
            return directory;
        }

        public String getFile() {
            return file;
        }

        @Override
        public String toString() {
            return directory + " --- " + file;
        }
    }



    /**
     * list the resources that match args[0]
     *
     * @param args
     *            args[0] is the pattern to match, or list all resources if
     *            there are no args
     */
    public static void main(final String[] args){
        Pattern pattern;
        if(args.length < 1){
            pattern = Pattern.compile(".*cheat_codes.*");
        } else{
            pattern = Pattern.compile(args[0]);
        }
        final Collection<String> list = ResourceList.getResources(pattern);
        for(final String name : list){
            System.out.println(name);
        }
    }
}
