/*
 * @(#)JavaAPILister.java	1.11	06/10/10
 *
 * Copyright  1990-2006 Sun Microsystems, Inc. All Rights Reserved.  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER  
 *   
 * This program is free software; you can redistribute it and/or  
 * modify it under the terms of the GNU General Public License version  
 * 2 only, as published by the Free Software Foundation.   
 *   
 * This program is distributed in the hope that it will be useful, but  
 * WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU  
 * General Public License version 2 for more details (a copy is  
 * included at /legal/license.txt).   
 *   
 * You should have received a copy of the GNU General Public License  
 * version 2 along with this work; if not, write to the Free Software  
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  
 * 02110-1301 USA   
 *   
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa  
 * Clara, CA 95054 or visit www.sun.com if you need additional  
 * information or have any questions. 
 *
 */

import consts.*;
import components.*;
import util.*;
import jcc.*;
import vm.*;

import java.lang.Integer;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

public class JavaAPILister extends LinkerUtil {
    int	             verbosity = 0;
    String	     commandOptions;

    ClassFileFinder  searchPath;

    ClassReader      rdr;

    ClassnameFilter  includeClasses;

    PrintStream	     memberOut = null;
    PrintStream	     classOut = null;

    String           outName = JavaCodeCompact.getOutName();
    String           romOutName = outName + "MemberFilterData.c";
    PrintStream      romOut = null;

    /* 'minput' should be the 'mout' generated by JavaAPILister
     * when jar files (contains classes) are taken as the input.
     * 'minput' and 'mout' contains a list of visiable classes
     * and their members. When 'minput' is set, other arguments
     * such as 'input', 'mout' and 'cout' are ignored.
     */
    String           minput = null;

    /*
     * The classes from the input jar file can have the same name as the
     * ROMized CDC classes. Use a separate loader to keep track of the 
     * classes from the input jar file. We don't want to mix them with the 
     * ROMized classes. This is to make sure we obtian the member 
     * information from input classes, not the ROMized classes.
     */
    components.ClassLoader apiloader =
        new components.ClassLoader("apilister", null);

    private static String
    memberFileHeader[] = {
	"#",
	"# %"+"W%	%"+"E%", // use constant expressions to fool SCCS
	"# List of visible class members",
	"#"
    };

    private static String
    classFileHeader[] = {
	"#",
	"# %"+"W%	%"+"E%", // use constant expressions to fool SCCS
	"# List of classes",
	"#"
    };

    private boolean
    readFile( String fileName, Vector classesProcessed ){

	if (rdr == null){
	    rdr = new ClassReader(0);
	}
	try {
	    if (fileName.endsWith(".zip") || fileName.endsWith(".jar")){ 
		rdr.readZip(fileName, classesProcessed);
	    } else { 
		rdr.readFile(fileName, classesProcessed);
	    }
	} catch ( IOException e ){
	    System.out.println(Localizer.getString("javacodecompact.could_not_read_file", fileName));
	    e.printStackTrace();
	    return false;
	}
	return true;
    }

    /* All classes read from the input jar file.  Include excluded 
      (non-visible), such as com.sun.* classes. */
    Vector  classesProcessed = new Vector();
    /* All visible classes. Sorted by class typeid. */
    LinkedList sortedClasses = new LinkedList();
    /* ROMized CDC classes corresponding to the classes in 'sortedClasses' */
    LinkedList sortedCVMClasses = new LinkedList();

    private void
    addToIncludes( String pattern ){
	if (includeClasses == null){
	    includeClasses = new ClassnameFilter();
	}
	includeClasses.includeName(pattern);
	if (verbosity > 0){
	    System.err.print("Adding to include list: ");
	    System.err.println(pattern);
	}
    }

    private boolean
    isClassIncluded(String classname){
	if (includeClasses == null){
	    return true;
	}
	return includeClasses.accept(null, classname);
    }

    private static BufferedPrintStream 
    openOutputFile(String name){
	File outFile = new File(name);
	try {
	    return new BufferedPrintStream( new FileOutputStream( outFile ) );
	}catch(java.io.IOException e){
	    System.err.println("Could not open file "+name);
	    return null;
	}
    }

    /* Process the command line options. The options should look like:
     *
     *   -listapi:include=java/*,include=javax/*,
     *            input=<jar1>:<jar2>,minput=<minput>,mout=<mout>,cout=<cout>
     *
     * 'include' - Visible packages
     * 'input'   - Jar file containing classes
     * 'minput'  - The class/member list ('mout' output) generated by
     *             JavaAPILister when input jar files are given
     * 'mout'    - A txt file containing class/member list extracted from
     *             the input classes
     * 'cout'    - A txt file containing only class list extracted from
     *             the input classes
     *
     * If 'minput' exists, all other options are ignored.
     *
     * Following is examples of what the 'minput' and 'mout' txt file look
     * like:
     *
     * CLASS java/util/Stack
     *    FIELDS
     *         elementData:[Ljava/lang/Object;
     *         elementCount:I
     *         capacityIncrement:I
     *    METHODS
     *         <init>:()V
     *         push:(Ljava/lang/Object;)Ljava/lang/Object;
     *         pop:()Ljava/lang/Object;
     *         peek:()Ljava/lang/Object;
     *         empty:()Z
     *         ...
     */
    private boolean processOptions(String args) {
        boolean success = true;
        Vector classesThisRead = new Vector();
        StringTokenizer options = new StringTokenizer(args.substring(9), ",");
        int num = options.countTokens();
        int si;

        if ((si = args.indexOf("minput=")) != -1) {
            /* minput exists, ignore the rest of the arguments */
            int ei = args.indexOf(',' , si);
            if (ei == -1) {
                minput = args.substring(si+7);
            } else {
                minput = args.substring(si+7, ei);
            }
            return success;
        }

        /* minput doesn't exist, process all arguments */
        for (int i = 0; i < num; i++) {
            String s = options.nextToken();
            if (s.startsWith("verbose=")) {
                if (s.substring(8).equals("true")) {
                    verbosity++;
		}
	    } else if (s.startsWith("include=")) {
                addToIncludes(s.substring(8));
	    } else if (s.startsWith("input=")) {
                String separator = System.getProperty("path.separator", ":");
                StringTokenizer st = new StringTokenizer(s.substring(6), separator);
                int numOfInput = st.countTokens();

                for (int j = 0; j < numOfInput; j++) {
                    String f = st.nextToken();
                    classesThisRead.clear();
                    if (!readFile(f, classesThisRead)) {
                        success = false;
		    }
                    classesProcessed.addAll(classesThisRead);
                    ClassTable.enterClasses(
                        classesThisRead.elements(), apiloader);
		}
	    } else if (s.startsWith("mout=")) {
                memberOut = openOutputFile(s.substring(5));
                if (memberOut == null) {
		    // message already written
		    success = false;
		}
	    } else if (s.startsWith("cout=")) {
                classOut = openOutputFile(s.substring(5));
                if (classOut == null){
		    // message already written
		    success = false;
		}
	    }
	}

        return success;
    }

    /*private void
    printHeader0(PrintStream out, String commentString) {
    	out.println(commentString + " DO NOT EDIT. This file is auto-generated by JCC from");
        out.println(commentString + " a fully ROMized build using following build options: ");
        out.println(commentString + "");
        out.println(commentString + "   CVM_INCLUDE_MIDP=true PCSL_DIR=<pcsl_dir>");
        out.println(commentString + "   MIDP_DIR=<midp_dir> J2ME_CLASSLIB=foundation");
        out.println(commentString + "   CVM_DUAL_STACK=true CVM_PRELOAD_LIB=true");
        out.println(commentString + "   CVM_MIDPFILTERINPUT=<cldc_classes_zip>");
        out.println(commentString + "   gen_member_filter");
        out.println(commentString + "");
        out.println(commentString + " The output files are generated/javavm/runtime/romjavaMemberFilterData.c,");
        out.println(commentString + " lib/MIDPFilterConfig.txt and lib/MIDPPermittedClasses.txt.");
	out.println(commentString + " If CLDC API is changed, this file needs to be re-generated.");
    }*/

    private void
    printHeader0(PrintStream out, String commentString) {
    	out.println(commentString + " DO NOT EDIT. This file is auto-generated by JCC.");
    }

    private void
    printFileHeader(PrintStream out, String header[]){
	int hLength = header.length;
	for (int i=0; i<hLength; i++){
	    out.println(header[i]);
	}
        out.println("#");
	printHeader0(out, "#");
        out.println("#");
    }

    /* Write filter data into a .c file. Class data are sorted
     * by typeid.
     */
    private void
    writeRomFileHeader(PrintStream out)
    {
        out.println("/*");
        printHeader0(out, " *");
        out.println(" * This file is a copy of the generated romjavaMemberFilterData.c.");
        out.println(" */");
        out.println();
        out.println("#include \"generated/javavm/runtime/romjava.h\"");
        out.println("#include \"javavm/include/dualstack_impl.h\"");
        out.println("");
    }

    /* Find the ROMizer version of class member, so we can access the
     * member typeid.
     */
    private ClassMemberInfo findROMMember(ClassInfo cdcClass,
                                          ClassMemberInfo[] cdccma,
                                          String membername,
                                          String membertype,
                                          boolean isMethod)
    {
        ClassMemberInfo cdccm = null;

        /* Search the ROMized class members to find a match */
        for (int j = 0; j < cdccma.length; j++) {
	    if (membername.equals(cdccma[j].name.string)) {
	        if (isMethod) {
                    if (membertype.equals(cdccma[j].type.string)) {
		        // match
                        return cdccma[j];
		    }
		} else {
		    // match 
                    return cdccma[j];
		}
	    }
	}

        /* Search the super class of the ROMized class for the member */
        ClassConstant supercc = cdcClass.superClass;
        if (supercc != null) {
            ClassInfo superClass =
                    ClassTable.lookupClass(supercc.name.string);
            ClassMemberInfo supercma[] = isMethod ?
                       ((ClassMemberInfo[])superClass.methods) :
		       ((ClassMemberInfo[])superClass.fields);
            cdccm = findROMMember(superClass, supercma,
                                  membername, membertype, isMethod);
            if (cdccm != null) {
                return cdccm;
            }
        }

        /* search the interfaces */
        int nInterfaces = cdcClass.interfaces.length;
        for (int i = 0; i < nInterfaces; i++) {
            ClassInfo interfaceClass = ClassTable.lookupClass(
                cdcClass.interfaces[i].name.string);
            ClassMemberInfo interfacecma[] = isMethod ?
                       ((ClassMemberInfo[])interfaceClass.methods) :
		       ((ClassMemberInfo[])interfaceClass.fields);
            cdccm = findROMMember(interfaceClass, interfacecma,
                                  membername, membertype, isMethod);
            if (cdccm != null) {
                return cdccm;
            }
        }

        return null;
    }

    /* 
     * Replace members in the vector with the members from the 
     * corresponding ROMized class.
     */
    private void replaceMembers(ClassInfo thisClass, Vector members,
                                boolean methods)
    {
        /* get the ROMized class */
        ClassInfo cdcClass = ClassTable.lookupClass(thisClass.className);
        if (cdcClass != null) {
            ClassMemberInfo cdccma[] = (methods) ? 
	               ((ClassMemberInfo[])cdcClass.methods) :
		       ((ClassMemberInfo[])cdcClass.fields);
            int size = members.size();
            for (int i = 0; i < size; i++) {
                ClassMemberInfo cm = (ClassMemberInfo)(members.get(i));
                ClassMemberInfo cdccm = findROMMember(
		    cdcClass, cdccma, cm.name.string, cm.type.string, methods);
                /* replace the member */
                if (cdccm != null) {
                members.set(i, cdccm);
	        }
	    }
	}
    }

    private void
    collectClassComponents(
	Vector components,
	ClassInfo thisClass,
	Vector classesAlreadySeen,
	boolean methods,
	boolean superIsFinal,
	boolean leafClass)
    {
	boolean classIsFinal;
	int     permissionMask;
	ClassMemberInfo cma[];
	// if this class has already been seen,
	// don't go looking at it again.
	if (classesAlreadySeen.contains(thisClass)){
	    return;
	}
	classesAlreadySeen.addElement(thisClass);

	// First look at all the elements in this class
	// and see if we want to add any of them. See if
	// they have already been added.
	permissionMask = Const.ACC_PUBLIC;
	classIsFinal = superIsFinal || 
	    ((thisClass.access & Const.ACC_FINAL) != 0);
	if (!classIsFinal){
	    permissionMask |= Const.ACC_PROTECTED;
	}
	cma = (methods) ? ((ClassMemberInfo[])thisClass.methods)
			: ((ClassMemberInfo[])thisClass.fields);
	if (cma != null){
            int i;
	    int nMembers = cma.length;
            ClassMemberInfo cm;
            
            /* start collecting qualified members */
	    for (i = 0; i < nMembers; i++){
		cm = cma[i];
		if ((cm.access & permissionMask) == 0)
		    continue;
		if (cm.name.string.equals("<clinit>"))
		    continue;
		if (!leafClass){
		    // this is a superclass or interface.
		    if (cm.isStaticMember()){
			// subclasses don't inherit our statics
			continue;
		    }
		    if (cm.name.string.equals("<init>")){
			// subclasses don't inherit our constructors
			continue;
		    }
		}
		// this looks like a candidate.
		// see if its already on the list.
		ClassMemberInfo otherCm;
		Enumeration componentsE = components.elements();
		boolean doPrint = true;
		while (componentsE.hasMoreElements()){
		    otherCm = (ClassMemberInfo)componentsE.nextElement();
		    if (!cm.name.string.equals(otherCm.name.string)){
			// names differ.
			// keep looking.
			continue;
		    }
		    // names the same
		    if (methods){
			if (!cm.type.string.equals(otherCm.type.string)){
			    // is a method and types are not the same.
			    // methods override on name and type.
			    // otherCm does not override this one.
			    continue;
			}
		    }
		    //
		    // otherCm is a field with the same name
		    // or a method with the same name and sig.
		    // cm has been overridden and should not be
		    // printed again.
		    doPrint = false;
		    break;
		}
		if (doPrint){
		    // we want to include this one on the list.
		    components.add(cm);
		}
	    }
	}

	// look at superclass
	String otherName;
	ClassInfo otherClass;
	ClassConstant superConstant = thisClass.superClass;
	if (superConstant != null){
	    // recurse into superclass
	    otherName = superConstant.name.string;
	    if ((otherClass = ClassTable.lookupClass(otherName, apiloader))
		!= null){
		collectClassComponents(components, otherClass,
		    classesAlreadySeen, methods, classIsFinal, false);
	    }
	}

	// look at interfaces. This should only matter
	// when the current class is an interface or abstract.
	if (thisClass.interfaces != null){
	    int nInterfaces = thisClass.interfaces.length;
	    for (int i=0; i<nInterfaces; i++){
		otherName = thisClass.interfaces[i].name.string;
		if ((otherClass = ClassTable.lookupClass(otherName, apiloader))
		    != null){
		// recurse into interfaces
		collectClassComponents(components, otherClass,
		    classesAlreadySeen, methods, classIsFinal, false);
		}
	    }
	}
    }

    private void
    printVector(String title, Object m[]){
	if (m.length == 0)
	    return;
	memberOut.println(title);

	for (int i = 0; i < m.length; i++){
	    ClassMemberInfo cm = (ClassMemberInfo)(m[i]);
	    memberOut.print('\t');
	    memberOut.print(cm.name.string);
	    memberOut.print(':');
	    memberOut.println(cm.type.string);
	}
    }

    private void
    setClassMembers(ClassInfo thisClass, Vector members,
                    boolean methods)
    {
        if (methods) {
            thisClass.methodtable = (MethodInfo[])members.toArray(new MethodInfo[0]);
        } else {
            thisClass.fieldtable = (FieldInfo[])members.toArray(new FieldInfo[0]);
        }
    }

    private void
    printClassComponents(ClassInfo thisClass)
    {
	Vector classesVisited;
	Vector methods;
        Vector fields;

	// print fields
	printVector("   FIELDS", thisClass.fieldtable);

	// print methods
	printVector("   METHODS", thisClass.methodtable);
    }

    private boolean
    includedAndVisible(ClassInfo c){
	String classname = c.className;
	int access = c.access;
	if (!isClassIncluded(classname)){
	    return false;
	}
	if ((access & Const.ACC_PUBLIC) == 0){
	    return false;
	}
	return true;
    }

    private void
    printClassInfo( ClassInfo c ){
	if (!includedAndVisible(c)){
	    if (verbosity > 0){
		System.err.print("Excluded class ");
		System.err.println(c.className);
	    }
	    return;
	}
	memberOut.print("\nCLASS ");
	memberOut.println(c.className);
	printClassComponents(c);
    }

    private void
    printClassName( ClassInfo c )
    {
	String name = c.className.replace('/','.');
	classOut.println(name);
    }

    public void writeRomFilter(PrintStream out)
    {
        int i, j;
        int nClasses = sortedClasses.size();
        ClassInfo thisClass;
        ClassInfo cdcClass;
        CVMClass cvmClass;
        String cName;

        writeRomFileHeader(romOut);

        /* first write all class member arrays */
        for (i = 0; i < nClasses; i++) {
            thisClass = (ClassInfo)sortedClasses.get(i);
            cvmClass = (CVMClass)sortedCVMClasses.get(i);
            cName = cvmClass.getNativeName();
            FieldInfo fa[] = thisClass.fieldtable;
            MethodInfo ma[] = thisClass.methodtable;
            int typeid, nameid;

            /* write the class's fields */
            if (fa.length > 0) {
                out.println("/* " + cName + " fields */");
                out.println("CVMFieldTypeID " + cName + "_f[] = {");
                for (j = 0; j < fa.length; j++) {
                    FieldInfo f = fa[j];
                    typeid = CVMDataType.parseSignature((f.type).toUTF());
                    nameid = CVMMemberNameEntry.lookupEnter((f.name).toUTF());
                    out.println("\tRAW_TYPEID(0x" + Integer.toHexString(nameid) +
                                ", 0x" + Integer.toHexString(typeid) + "), ");
	        }
                out.println("};");
	    }

            /* write the class' methods */
	    if (ma.length > 0) {
                out.println("/* " + cName + " methods */");
                out.println("CVMMethodTypeID " + cName + "_m[] = {");
                for (j = 0; j < ma.length; j++) {
                    MethodInfo m = ma[j];
                    CVMMethodInfo cm = (CVMMethodInfo)m.vmMethodInfo;
                    typeid = cm.sigType.entryNo;
                    nameid = CVMMemberNameEntry.lookupEnter(
                                    (cm.method.name).toUTF());
                    out.println("\tRAW_TYPEID(0x" + Integer.toHexString(nameid) +
                                ", 0x" + Integer.toHexString(typeid) + "), ");
	        }
                out.println("};");
	    }
        }

        /* write ClassRestrictions data struct */
        out.println("\n/* The dual stack member filter. */");
        out.println("struct ClassRestrictionElement cre[] = {");
        for (i = 0; i < nClasses; i++) {
            thisClass = (ClassInfo)sortedClasses.get(i);
            cvmClass = (CVMClass)sortedCVMClasses.get(i);
            cName = cvmClass.getNativeName();
            /* class typeid */
            out.print("{0x" + Integer.toHexString(cvmClass.classid()) + ", ");
            /* number of methods */
            out.print(thisClass.methodtable.length + ", ");
            /* number of fields */
            out.print(thisClass.fieldtable.length + ", ");
            /* method table */
            if (thisClass.methodtable.length == 0) {
                out.print("NULL /* " + cName + "_m */, ");
	    } else {
                out.print(cName + "_m, ");
	    }
            /* field table */
            if (thisClass.fieldtable.length == 0) {
                out.println("NULL /* " + cName + "_f */},");
	    } else {
                out.println(cName + "_f},");
	    }
	}
        out.println("};");

        /* write ClassRestrictions data struct */
        out.print("const struct ClassRestrictions CVMdualStackMemberFilter = {");
        out.print(nClasses + ", ");
        out.println("cre};");

        out.flush();
    }

    private void writeMemberFile()
    {
        if (memberOut == null) {
            /* mout is not set. Nothing to do here. */
            return;
        }

        int nClasses = classesProcessed.size();
        printFileHeader(memberOut, memberFileHeader);
        for (int i = 0; i < nClasses; i++) {
            ClassInfo c = (ClassInfo)classesProcessed.get(i);
            if (!includedAndVisible(c)) {
                continue; /* excluded. Skip this class. */
            }
            memberOut.print("\nCLASS ");
	    memberOut.println(c.className);
            printClassComponents(c);
	}
        memberOut.flush();
    }

    private void writeClassFile()
    {
        if (classOut != null){
	    printFileHeader(classOut, classFileHeader);
	    int nClasses = classesProcessed.size();
	    for (int i = 0; i < nClasses; i++){
                ClassInfo c = (ClassInfo)classesProcessed.get(i);
                if (!includedAndVisible(c)) {
                    continue; /* excluded. Skip this class. */
                }
		printClassName(c);
	    }
	    classOut.flush();
	}
    }

    /*
     * Collect class members and store them in 'fieldtable' and
     * 'methodtable'.
     */
    private void processClasses()
    {
        if (minput != null) {
            /*
             * Parse the minput and create ClassInfo for each
             * class in the list.
             */
            parseMInput(minput);
            return;
	}

        /*
         * The classes are from a input jar file. Process
         * classes and collect visiable members.
         */
        int num = classesProcessed.size();
        for (int i = 0; i < num; i++) {
            ClassInfo thisClass = (ClassInfo)classesProcessed.get(i);
            if (!includedAndVisible(thisClass)){
	        if (verbosity > 0){
		    continue; /* skip excluded class */
	        }
	    }
            /* collect fields */
            Vector fields = new Vector();
            collectClassComponents(fields, thisClass, new Vector(), 
			           false, false, true);
            /* replace the fields with the ROMized version in order
             * get the typeid info. */
            replaceMembers(thisClass, fields, false);
            /* set 'fieldtable' */
            setClassMembers(thisClass, fields, false);

            /* collect methods */
            Vector methods = new Vector();
            collectClassComponents(methods, thisClass, new Vector(), 
			           true, false, true);
            /* replace the methods with ROMized verison in order
             * to get the typeid info. */
            replaceMembers(thisClass, methods, true);
            /* set 'methodtable' */
            setClassMembers(thisClass, methods, true);
	}
    }

    /*
     * Sort classes in 'classesProcessed' Vector by typeid, and add them
     * to 'sortedClasses' LinkedList. Only visible classes are added to
     * 'sortedClasses'. Also add the corresponding internal VM class to 
     * 'sortedCVMClasses' in the same order.
     */
    private boolean sortClasses()
    {
        int nClasses = classesProcessed.size();
        for (int i = 0; i < nClasses; i++) {
            ClassInfo thisClass = (ClassInfo)classesProcessed.get(i);
            if (!includedAndVisible(thisClass)) {
                continue; /* excluded. Skip this class. */
            }

            ClassInfo cdcClass;
            CVMClass cvmClass;
            int id;
            int nSortedClasses;
            int j;
            cdcClass = ClassTable.lookupClass(thisClass.className);
            if (cdcClass == null) {
                sortedClasses.clear();
                sortedCVMClasses.clear();
                return false;
	    }
            cvmClass = (CVMClass)cdcClass.vmClass;
            id = cvmClass.classid();
            nSortedClasses = sortedCVMClasses.size();

            for (j = 0; j < nSortedClasses; j++) {
                if (id < ((CVMClass)sortedCVMClasses.get(j)).classid()) {
                    break; /* found location */
		}
            }
            /* add to the sorted linked lists */
            sortedClasses.add(j, thisClass);
            sortedCVMClasses.add(j, cvmClass);
	}
        return true;
    }

    // keywords in easy-to-manipulate integer form
    private static final int NOKEY = 0;
    private static final int CLASS = 1;
    private static final int FIELDS = 2;
    private static final int METHODS = 3;
    private static Hashtable keywords = new Hashtable();

    static {
        keywords.put("",        new Integer(NOKEY));
        keywords.put("CLASS",   new Integer(CLASS));
        keywords.put("FIELDS",  new Integer(FIELDS));
        keywords.put("METHODS", new Integer(METHODS));
    }

    /* Get the integer form of the keyword */
    private static int keyword(String word) {
	Integer n = (Integer)keywords.get(word);
        if (n != null) {
            return n.intValue();
        } else {
	    return NOKEY;
        }
    }

    /* Get the next word from the 'in' stream */
    private String
    nextWord(StreamTokenizer in) throws Exception{
	int token = in.nextToken();
	if (token == in.TT_EOF){
	    throw new EOFException("EOF");
	}
	if (token != in.TT_WORD){
	    throw new Exception("token type");
	}
	return in.sval;
    }

    private int
    expecting(StreamTokenizer in, int token1, int token2, int token3)
                                                throws Exception{
	int key;
	key = keyword(nextWord(in));
	if (key != token1 && key != token2 && key != token3){
	    throw new Exception("token value ".concat(in.sval));
	}
	return key;
    }

    static private void
    printStringArray(String name, String val[]){
	if (val == null || val.length == 0)
	    return;
	System.out.println(name);
	int l = val.length;
	for(int i=0; i<l; i++){
	    System.out.print('\t');
	    System.out.println(val[i]);
	}
    }

    /*
     * Parse the input class and member list. Create a ClassInfo
     * for the corresponding class. 
     */
    private void
    parseClass(StreamTokenizer in) throws Exception {
	String classname = null;
	String word = null;
	int    keyword;
	int    token;
	LinkedList methods = new LinkedList();
	LinkedList fields = new LinkedList();
	try{
	    // parse CLASS and classname
	    expecting(in, CLASS, -1, -1);
	    classname = nextWord(in);
	    while(true){
		// parse FIELDS or METHODS
		keyword = expecting(in, FIELDS, METHODS, CLASS);
		LinkedList thislist;
		if (keyword == CLASS){
		    // beginning of next class signals the end of this one.
		    in.pushBack();
		    break;
		}
		thislist = (keyword == FIELDS)? fields : methods;
		while(true){
		    String memberName = nextWord(in);
		    // see if this name is a keyword.
		    // since the memberNames, if properly formed,
		    // must contain :, there should be no danger of
		    // confusion.
		    keyword = keyword(memberName);
		    if (keyword != NOKEY){
			in.pushBack();
			break;
		    }
		    // make sure we have our :
		    if (memberName.indexOf(':') == -1){
			throw new Exception("format error in ".concat(memberName));
		    }
		    thislist.add(memberName);
		}
	    }
	}finally{
	    if (classname != null){
		// here, using empty as a prototype for String[]
                String empty[] = new String[0];
		String methodstrings[] = (String[])methods.toArray(empty);
		String fieldstrings[] = (String[])fields.toArray(empty);
		if (verbosity > 0){
		    System.out.print("CLASS ");
		    System.out.println(classname);
		    printStringArray("    FIELDS", fieldstrings);
		    printStringArray("    METHODS", methodstrings);
		}

                enterClassInfo(classname, methodstrings, fieldstrings);
	    }
	}
    }

    /*
     * 'ci' is the ClassInfo created for a class name in the minput.
     * 'cdcClass' is the corresponding ROMized class.  For each member
     * in the 'members' array, find the ClassMemberInfo
     * from the ROMized 'cdcClass'. Set the methodtable (or
     * fieldtable) of 'ci' with the ClassMemberInfo vector.
     */
    private void enterMemberInfo(ClassInfo ci, ClassInfo cdcClass,
                                 String members[], boolean isMethod) {
        int length = members.length;
        Vector mv = new Vector();
        ClassMemberInfo cdcma[] = (isMethod) ?
	               ((ClassMemberInfo[])cdcClass.methods) :
		       ((ClassMemberInfo[])cdcClass.fields);
        for (int i = 0; i < length; i++) {
            int idx = members[i].indexOf(':');
            String membername, membertype;
            membername = members[i].substring(0, idx);
            membertype = members[i].substring(idx + 1);
            ClassMemberInfo f = findROMMember(
                cdcClass, cdcma, membername, membertype, isMethod);
            mv.add(f);
        }
        setClassMembers(ci, mv, isMethod);
    }

    /* Create a new ClassInfo and add it to te 'classesProcessed' vector. */
    private void enterClassInfo(String classname, 
                                String methods[],
                                String fields[])
                                throws ClassNotFoundException
    {
        ClassInfo cdcClass = ClassTable.lookupClass(classname);
        if (cdcClass == null) {
            /*
             * The ROMized class doesn't exist. This probably is not
             * a fully ROMized build, and we only support ROMized build.
             * Remove all classes from the 'classesProcessed' vector
             * and bail out.
             */
            classesProcessed.removeAllElements();
            throw new ClassNotFoundException("Can't find ROMized " + classname);
        }

        /* Create the ClassInfo, which only contains className,
           methods and fields information that we care about.
	 */
        ClassInfo ci = new ClassInfo(false);
        ci.className = classname;
        ci.access = Const.ACC_PUBLIC;

        /* enter fields info */
        enterMemberInfo(ci, cdcClass, fields, false);

        /* process methods */
        enterMemberInfo(ci, cdcClass, methods, true);

        /* add the class to the 'classesProcessed' vector */
        classesProcessed.add(ci);
    }

    /*
     * Parse the 'minput'. Create ClassInfo for each class in
     * list. For each class member, find the ClassMemberInfo
     * from the corresponding ROMized class and set the ClassInfo's
     * methodtable and fieldtable with the collected ClassMemberInfo.
     */
    private void parseMInput(String minput) {
        try {
            FileReader inFile = new FileReader(minput);
            StreamTokenizer in = new StreamTokenizer(new BufferedReader(inFile));
            in.commentChar('#');
   	    in.slashSlashComments(false);
	    in.eolIsSignificant(false);
	    in.wordChars('a','z');
	    in.wordChars('A','[');
	    in.wordChars('0','9');
	    in.wordChars('<','<'); // appears in name <init>
	    in.wordChars('>','>');
	    in.wordChars('_','_');
	    in.wordChars(':',':'); // make special separator wordChar
 	    in.wordChars(';',';'); // make all parts of a signature wordChar
	    in.wordChars('(',')');
	    in.wordChars('/','/'); // make name component separator wordChar
	    in.whitespaceChars(' ',' '); // ignore space
	    in.whitespaceChars('\t','\t'); // ignore tabs

            try{
	        while(true){
		    parseClass(in);
	        }
  	    }catch(EOFException e){
	        // normal termination. do nothing
            }catch(ClassNotFoundException e) {
                return;
            }catch(IOException e){
	        e.printStackTrace();
                return;
            }catch(Exception e){
                e.printStackTrace();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
	}
    }

    public boolean process( String clist ) throws Exception {
        if (clist != null) {
	    commandOptions = clist;
	    if (! processOptions( clist )){
	        // malformed command-line argument or file read error
	        return false;
	    }
        }

        if (minput != null || memberOut != null) {
            /*
	     * Process classes and collect visible members. We use
             * the 'fieldtable' and 'methodtable' to store collected
             * members.
             */
            processClasses();

            /*
             * Sort classes in typeid order
             */
            sortClasses();

            /*
             * Write filter data into .c file
             */
            romOut =  openOutputFile(romOutName);
            writeRomFilter(romOut);
            /* Add .c file to romjavaList */
            FileWriter listOut = new FileWriter(outName + "List", true);
            listOut.write(romOutName, 0, romOutName.length());
            listOut.close();

	    /*
	     * Write the member .txt file.
	     */
	    writeMemberFile();
        }

	/*
	 * Now if a classlist output is specified do it too.
	 */
	writeClassFile();

	return true;
    }

    public static void main( String clist[] ){
	boolean success;
	try {
	    try {
		success = new JavaAPILister().process( clist[0] );
	    }finally{
		System.out.flush();
		System.err.flush();
	    }
	}catch (Throwable t){
	    success = false;
	    t.printStackTrace();
	}
	if (!success){
	    // process threw error or failed
	    System.exit(1);
	}
	return;
    }
}
