package org.hawkular.helpers.rest_docs_generator;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiProperty;
import org.hawkular.helpers.rest_docs_generator.model.ErrorCode;
import org.hawkular.helpers.rest_docs_generator.model.PApi;
import org.hawkular.helpers.rest_docs_generator.model.PClass;
import org.hawkular.helpers.rest_docs_generator.model.PData;
import org.hawkular.helpers.rest_docs_generator.model.PMethod;
import org.hawkular.helpers.rest_docs_generator.model.PParam;
import org.hawkular.helpers.rest_docs_generator.model.PProperty;
import org.hawkular.helpers.rest_docs_generator.model.PTypeInfo;
import org.hawkular.helpers.rest_docs_generator.model.ParamType;
import org.jboss.resteasy.annotations.GZIP;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Processor for JAX-RS classes
 * @author Heiko W. Rupp
 */

@SupportedOptions({Processor.TARGET_DIRECTORY, Processor.VERBOSE_KEY, Processor.MODEL_PACKAGE_KEY,
        Processor.SKIP_PACKAGE_KEY, Processor.OUTPUT_FORMAT_KEY, Processor.HEADER_LINE_KEY, Processor.ABORT_PATTERN_KEY})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(value = {"com.wordnik.swagger.annotations.*","javax.ws.rs.*",
        "javax.xml.bind.annotation.XmlRootElement"})
public class Processor extends AbstractProcessor {

    // keywords for processor configuration
    public static final String MODEL_PACKAGE_KEY  = "modelPkg";
    public static final String SKIP_PACKAGE_KEY  = "skipPkg";
    public static final String OUTPUT_FORMAT_KEY = "outputFormat";
    public static final String HEADER_LINE_KEY = "headerLine";
    public static final String ABORT_PATTERN_KEY = "abortPattern";
    public static final String VERBOSE_KEY = "verbose";

    private static final String JAVAX_WS_RS = "javax.ws.rs";
    private static final String[] HTTP_METHODS = {"GET","PUT","POST","HEAD","DELETE","OPTIONS"};
    private static final String[] PARAM_SKIP_ANNOTATIONS = {"javax.ws.rs.core.UriInfo","javax.ws.rs.core.HttpHeaders",
            "javax.servlet.http.HttpServletRequest","javax.ws.rs.core.Request","javax.ws.rs.container.Suspended"};
    private static final String API_OUT = "rest-api-out";
    public static final String TARGET_DIRECTORY = "targetDirectory";

    public String modelPackage = null;
    public String skipPackage  = null;
    private String outputFormat;
    private String headerLine = "REST-API";
    private String abortPattern;

    Logger log = Logger.getLogger(getClass().getName());

    private String targetDirectory;
    boolean verbose = false;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        Map<String,String>options =  processingEnv.getOptions();
        targetDirectory = getProcessorOption(options,TARGET_DIRECTORY,"target/docs");
        if (options.containsKey(VERBOSE_KEY)) {
            if (options.get(VERBOSE_KEY).toLowerCase().startsWith("t"))
            verbose = true;
        }
        modelPackage = getProcessorOption(options,MODEL_PACKAGE_KEY,null);
        skipPackage = getProcessorOption(options,SKIP_PACKAGE_KEY,null);
        outputFormat = getProcessorOption(options, OUTPUT_FORMAT_KEY,"adoc");
        headerLine = getProcessorOption(options, HEADER_LINE_KEY, "REST-API" );
        abortPattern = getProcessorOption(options, ABORT_PATTERN_KEY,null);
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        // We are invoked twice, but do our work already in the first round, so we can stop
        if (roundEnv.processingOver()) {
            return false;
        }

        if (verbose) {
            StringBuilder builder = new StringBuilder("=== Looking at\n");
            for (Element e : roundEnv.getRootElements()) {
                builder.append("  ").append(e.toString()).append("\n");
            }
            log.info(builder.toString());
        }

        // If an abort pattern is set, look for it and stop processing if found
        if (abortPattern != null && !abortPattern.isEmpty()) {
            for (Element e : roundEnv.getRootElements()) {
                if (e.toString().contains(abortPattern)) {
                    log.info("Found abort pattern, abort processing round");
                    return false;
                }
            }
        }

        PApi papi = new PApi();
        papi.name = headerLine;

        // Loop over all classes, this does the work
        for (javax.lang.model.element.Element t : roundEnv.getRootElements()) {
            processClass(papi, (TypeElement)t);
        }

        log.info("== Model generation done, writing output");

        if (verbose) {
            log.info("== Model: " + papi);
        }


        DataWriter dw;
        if (outputFormat.contains("adoc")) {
            File f = getOutputFile(".adoc");
            dw = new AsciiDocWriter();
            dw.write(f, papi);
        }

        if (outputFormat.contains("xml")) {
            File f = getOutputFile(".xml");
            dw = new XmlWriter();
            dw.write(f, papi);
        }

        return false;
    }


    /**
     * Get the output file from the passed suffix and create target directory on the fly if needed
     * @param suffix Suffix of the file name
     * @return a File object for the output file.
     */
    private File getOutputFile(String suffix) {
        File f ;
        String name = API_OUT + suffix;
        if (targetDirectory!=null) {
            File targetDir = new File(targetDirectory);
            if (!targetDir.exists()) {
                boolean success = targetDir.mkdirs();
                if (!success)
                    log.severe(("Creation of target directory " + targetDirectory + " failed"));
            }


            f = new File(targetDir, name);
        }
        else
            f = new File(name);

        if (verbose) {
            log.info("== File is " + f.getAbsolutePath());
        }
        return f;
    }

    /**
     * Process one single class
     * @param pApi Api to add the class to
     * @param classElementIn TypeElement describing the class
     */
    private void processClass(PApi pApi, TypeElement classElementIn) {

        log.fine("Looking at " + classElementIn.getQualifiedName().toString());
        if (skipPackage!=null && classElementIn.getQualifiedName().toString().startsWith(skipPackage)) {
            log.fine(" .. skipping ..");
            return;
        }
        // Process the data classes
        if (classElementIn.getAnnotation(ApiClass.class)!=null) {
            processDataClass(pApi, classElementIn);
            return;
        }

        // No data class, so it is the api.
        Path basePath = classElementIn.getAnnotation(Path.class);
        if (basePath==null || basePath.value().isEmpty()) {
            log.fine("No @Path found on " + classElementIn.getQualifiedName() + " - skipping");
            return;
        }

        log.info("Working on " + classElementIn.toString());
        PClass pClass = new PClass();
        pClass.name = classElementIn.toString();
        String value = basePath.value();
        value = cleanOutPath(value);
        pClass.path = value;
        Api api = classElementIn.getAnnotation(Api.class);
        if (api!=null) {
            pClass.shortDesc = api.value();
            pClass.description= api.description();
            pClass.basePath = api.basePath();
        }

        Produces produces = classElementIn.getAnnotation(Produces.class);
        if (produces!=null) {
            String[] types = produces.value();
            pClass.produces = new ArrayList<>(types.length);
            Collections.addAll(pClass.produces, types);
        }

        pApi.classes.add(pClass);

        // Loop over the methods on this class
        for (ExecutableElement executableElement : ElementFilter.methodsIn(classElementIn.getEnclosedElements())) {
            processMethods(pClass, executableElement);
        }
    }


    /**
     * Process a single method on a class
     * @param pClass The class to add the method to
     * @param td One Type element for the method
     */
    private void processMethods(PClass pClass, ExecutableElement td) {

        log.fine("  Looking at method " + td.getSimpleName().toString());

        Path pathAnnotation = td.getAnnotation(Path.class);
        if (pathAnnotation==null) {
            return;
        }
        String path = pathAnnotation.value();
        path = cleanOutPath(path);

        PMethod pMethod = new PMethod();
        pClass.methods.add(pMethod);
        pMethod.path = path;

        Name elementName = td.getSimpleName();
        pMethod.name = elementName.toString();
        pMethod.method = getHttpMethod(td.getAnnotationMirrors());
        GZIP gzip = td.getAnnotation(GZIP.class);
        if (gzip!=null) {
            pMethod.gzip= true;
        }
        ApiOperation apiOperation = td.getAnnotation(ApiOperation.class);
        String responseClass = null;
        if (apiOperation!=null) {
            pMethod.description = apiOperation.value();
            if (!apiOperation.responseClass().equals("void")) {
                responseClass = apiOperation.responseClass();
                if (apiOperation.multiValueResponse())
                    responseClass = responseClass + " (multi)";
            }

            pMethod.notes = apiOperation.notes();

        }

        if (responseClass == null) {
            responseClass = td.getReturnType().toString();
        }

        // TODO can we somehow make the responseClass fully qualified, so that the link generation works?
        pMethod.returnType = constructTypeInfo(responseClass);

        // Loop over the parameters
        processParams(pMethod, td);

        processErrors(pMethod,td);

    }

    /**
     * Process the parameters of a method.
     * @param doc Model Method to add the output to
     * @param methodElement Method to look for parameters
     */
    private void processParams(PMethod doc, ExecutableElement methodElement) {
        for (VariableElement paramElement : methodElement.getParameters()) {
            TypeMirror t = paramElement.asType();
            if (skipParamType(t))
                continue;
            PParam pParam = new PParam();
            doc.params.add(pParam);
            // determine name
            String name;
            ParamType paramType= ParamType.BODY;
            PathParam pp = paramElement.getAnnotation(PathParam.class);
            QueryParam qp = paramElement.getAnnotation(QueryParam.class);
            ApiParam ap = paramElement.getAnnotation(ApiParam.class);
            FormParam fp = paramElement.getAnnotation(FormParam.class);
            if (pp != null) {
                name = pp.value();
                paramType=ParamType.PATH;
            }
            else if (qp!=null) {
                name = qp.value();
                paramType=ParamType.QUERY;
            }
            else if (fp!=null) {
                name = fp.value();
                paramType = ParamType.FORM;
            }
            else if (ap!=null)
                name = ap.name();
            else {
                Name nameElement = paramElement.getSimpleName();
                name = nameElement.toString();
            }
            pParam.name = name;
            pParam.paramType = paramType;

            ApiParam apiParam = paramElement.getAnnotation(ApiParam.class);
            if (apiParam!=null) {
                pParam.description = apiParam.value();
                boolean required = apiParam.required();
                if (pp!=null || paramType.equals(ParamType.BODY)) {// PathParams are always required
                    required = true;
                }
                pParam.required = required;

                String allowedValues = apiParam.allowableValues();
                if (allowedValues!=null && !allowedValues.isEmpty()) {
                    pParam.allowableValues = allowedValues;
                }

            }
            String defaultValue;
            DefaultValue dva = paramElement.getAnnotation(DefaultValue.class);
            if (dva!=null) {
                defaultValue = dva.value();
            }
            else if (ap!=null) {
                defaultValue = ap.defaultValue();
            }
            else {
                defaultValue = "-none-";
            }

            if (defaultValue!=null) {
                pParam.defaultValue = defaultValue;
            }

            String typeString = t.toString();
            pParam.typeInfo = constructTypeInfo(typeString);
        }
    }

    private PTypeInfo constructTypeInfo(String typeString) {
        String typeId = typeString;
        if (typeString.contains("java.lang.")) {
            typeString = typeString.replaceAll("java\\.lang\\.","");
        }
        else if (typeString.contains("java.util.")) {
            typeString = typeString.replaceAll("java\\.util\\.","");
        }

        if (modelPackage!=null && typeString.contains(modelPackage)) {
            String mps = modelPackage.endsWith(".") ? modelPackage : modelPackage + ".";
            // For a generic collection we need to find the "inner type" and link to it
            int offset = typeString.contains("<") ? typeString.indexOf('<') +1 : 0;
            String restType = typeString.substring(offset + modelPackage.length()+1);
            if (restType.endsWith(">"))
                restType = restType.substring(0,restType.length()-1);
            log.info("REST TYPE " + restType);
            typeString = typeString.replace(mps,"");
            typeId  = "..." + restType;
        }

        PTypeInfo pTypeInfo = new PTypeInfo(typeString,typeId);
        return pTypeInfo;
    }

    /**
     * Look at the ApiError(s) annotations and populate the output
     * @param pMethod PMethod to add to
     * @param methodElement Method declaration to look at
     */
    private void processErrors(PMethod pMethod, ExecutableElement methodElement) {
        ApiError ae = methodElement.getAnnotation(ApiError.class);
        processError(pMethod,ae);
        ApiErrors aes = methodElement.getAnnotation(ApiErrors.class);
        if (aes != null) {
            for (ApiError ae2 : aes.value()) {
                processError(pMethod,ae2);
            }
        }
    }

    /**
     * Process a single @ApiError
     * @param doc XML Document to add
     * @param ae ApiError annotation to evaluate
     */
    private void processError(PMethod doc, ApiError ae) {
        if (ae==null)
            return;

        ErrorCode ec = new ErrorCode(ae.code(),ae.reason());
        doc.errors.add(ec);
    }

    private void processDataClass(PApi doc, TypeElement classElementIn) {

        String pkg = classElementIn.toString();
        log.fine("Looking at " + pkg);
        if ((modelPackage!=null &&!pkg.startsWith(modelPackage)) ||
                (skipPackage!=null && pkg.startsWith(skipPackage))) {
            log.fine(" skipping as it does not meet the required package");
            return;
        }

        PData pData = new PData();
        doc.data.add(pData);
        pData.name = classElementIn.getSimpleName().toString();
        pData.nameId = "..." + classElementIn.getSimpleName().toString();
        ApiClass api = classElementIn.getAnnotation(ApiClass.class);
        if (api!=null) {
            pData.shortDescription = api.value();
            pData.description = api.description();
        }
        // Determine the name of how the elements of this class are named in the XML / JSON output
        XmlRootElement rootElement = classElementIn.getAnnotation(XmlRootElement.class);
        String objectName;
        if (rootElement!=null) {
            objectName = rootElement.name();
        }
        else {
            objectName = classElementIn.getSimpleName().toString();
        }
        pData.objectName = objectName;
        processDataClassProperties(pData, classElementIn);

    }

    private void processDataClassProperties(PData doc, TypeElement classElementIn) {
        // Now look at the properties by processing the getters
        for (ExecutableElement m : ElementFilter.methodsIn(classElementIn.getEnclosedElements())) {

            String mName = m.getSimpleName().toString();
            if (mName.startsWith("get") || mName.startsWith("is")) {
                PProperty pProperty = new PProperty();
                doc.properties.add(pProperty);
                String pName;
                if (mName.startsWith("get")) {
                    pName = mName.substring(3);
                } else {
                    pName = mName.substring(2);
                }
                pName = pName.substring(0,1).toLowerCase() + pName.substring(1);

                pProperty.name = pName;
                ApiProperty ap = m.getAnnotation(ApiProperty.class);
                if (ap!=null) {
                    pProperty.description = ap.value();
                    pProperty.notes = ap.notes();
                }

                TypeMirror returnTypeMirror = m.getReturnType();
                //  for types in the modelPackage or java.lang, remove the fqdn
                String typeName = returnTypeMirror.toString();
                if (modelPackage!=null && typeName.contains(modelPackage)) {
                    typeName = typeName.replace(modelPackage+".","");
                }
                if (typeName.contains("java.lang.")) {
                    typeName = typeName.replaceAll("java\\.lang\\.", "");
                }
                if (typeName.contains("java.util.")) {
                    typeName = typeName.replaceAll("java\\.util\\.","");
                }
                pProperty.type = typeName;
            }
        }
    }

    /**
     * Determine if the passed mirror belongs to an annotation that denotes a parameter to be skipped
     * @param t Type to analyze
     * @return True if the type matches the blacklist
     */
    private boolean skipParamType(TypeMirror t) {
        String name = t.toString();
        boolean skip=false;
        for (String toSkip : PARAM_SKIP_ANNOTATIONS) {
            if (toSkip.equals(name)) {
                skip=true;
                break;
            }
        }
        return skip;
    }

    /**
     * Determine the http method (@GET, @PUT etc.) from the list of annotations on the method
     * @param annotationMirrors mirrors for the method
     * @return The http method string or null if it can not be determined
     */
    private String getHttpMethod(Collection<? extends AnnotationMirror> annotationMirrors) {
        for (AnnotationMirror am : annotationMirrors) {
            javax.lang.model.element.Element element = am.getAnnotationType().asElement();
            String pName = element.getEnclosingElement().toString();
            String cName = element.getSimpleName().toString();
            if (pName.startsWith(JAVAX_WS_RS)) {
                for (String name : HTTP_METHODS) {
                    if (cName.equals(name)) {
                        return name;
                    }
                }
            }
        }
        return "GET";
    }



    private String cleanOutPath(String in) {
        if (in.equals("/"))
            return "";

        if (in.startsWith("/"))
            in = in.substring(1);
        if (in.endsWith("/"))
            in = in.substring(0,in.length()-1);

        return in;
    }

    private String getProcessorOption(Map<String, String> options, String headerName, String defaultValue) {
        if (options.containsKey(headerName)) {
            return  options.get(headerName);
        }
        else {
            return defaultValue;
        }

    }


}
