package org.jbpm.designer.bpmn2.resource;


import org.eclipse.bpmn2.*;
import org.eclipse.bpmn2.util.Bpmn2ResourceImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.xmi.XMLHelper;
import org.eclipse.emf.ecore.xmi.XMLLoad;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.XMLSave;
import org.eclipse.emf.ecore.xmi.impl.XMLLoadImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.eclipse.emf.ecore.resource.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class JBPMBpmn2ResourceImpl extends Bpmn2ResourceImpl {

    public HashMap xmlNameToFeatureMap = new HashMap();

	public JBPMBpmn2ResourceImpl(URI uri) {
		super(uri);
        this.getDefaultLoadOptions().put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, true);
        this.getDefaultLoadOptions().put(XMLResource.OPTION_DISABLE_NOTIFY, true);
        this.getDefaultLoadOptions().put(XMLResource.OPTION_USE_XML_NAME_TO_FEATURE_MAP, xmlNameToFeatureMap);

        this.getDefaultSaveOptions().put(XMLResource.OPTION_ENCODING, "UTF-8");
        this.getDefaultSaveOptions().put(XMLResource.OPTION_PROCESS_DANGLING_HREF, XMLResource.OPTION_PROCESS_DANGLING_HREF_DISCARD);
	}
	
	@Override
    protected XMLSave createXMLSave() {
        prepareSave();
        return new JBPMXMLSaveImpl(createXMLHelper()) {
            @Override
            protected boolean shouldSaveFeature(EObject o, EStructuralFeature f) {
                if (Bpmn2Package.eINSTANCE.getDocumentation_Text().equals(f))
                    return false;
                if (Bpmn2Package.eINSTANCE.getFormalExpression_Body().equals(f))
                    return false;
                return super.shouldSaveFeature(o, f);
            }
        };
    }

    /**
     * Override this method to hook in our own XmlHandler
     */
    @Override
    protected XMLLoad createXMLLoad() {
        return new XMLLoadImpl(createXMLHelper()) {
            Bpmn2ModelerXmlHandler handler;

            @Override
            protected DefaultHandler makeDefaultHandler() {
                handler = new Bpmn2ModelerXmlHandler(resource, helper, options);
                return handler;
            }

            @Override
            public void load(XMLResource resource, InputStream inputStream, Map<?, ?> options) throws IOException {
                try {
                    super.load(resource, inputStream, options);
                }
                catch (Exception e) {
                    DiagnosticWrappedException error = new DiagnosticWrappedException(e);
                    error.setLine(handler.getLineNumber());
                    error.setColumn(handler.getColumnNumber());
                    error.setLocation(handler.getLocation());
                    resource.getErrors().add(error);
                }
            }
        };
    }

    class DiagnosticWrappedException extends WrappedException implements Resource.Diagnostic {
        private static final long serialVersionUID = 1L;
        private String location;
        private int column;
        private int line;

        public DiagnosticWrappedException(Exception exception) {
            super(exception);
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getLocation() {
            return location;
        }

        public void setColumn(int column) {
            this.column = column;;
        }

        public int getColumn() {
            return column;
        }

        public void setLine(int line) {
            this.line = line;
        }

        public int getLine() {
            return line;
        }
    }

    /**
     * We need to extend the standard SAXXMLHandler to hook into the handling of
     * attribute references which may be either simple ID Strings or QNames.
     * We'll search through all of the objects' IDs first to find the one we're
     * looking for. If not, we'll try a QName search.
     */
    protected static class Bpmn2ModelerXmlHandler extends BpmnXmlHandler {
        public Bpmn2ModelerXmlHandler(XMLResource xmiResource, XMLHelper helper, Map<?, ?> options) {
            super(xmiResource, helper, options);
        }

        public int getLineNumber() {
            return super.getLineNumber();
        }

        public int getColumnNumber() {
            return super.getColumnNumber();
        }

        public String getLocation() {
            return super.getLocation();
        }

    }

    public static EObject createStringWrapper(String value) {
        DynamicEObjectImpl de = new DynamicEObjectImpl() {
            // prevent owners from trying to resolve this thing - it's just a string!
            public boolean eIsProxy() {
                return false;
            }

            @Override
            public boolean equals(Object object) {
                if (object instanceof DynamicEObjectImpl) {
                    DynamicEObjectImpl that = (DynamicEObjectImpl) object;
                    if (eProxyURI()==null) {
                        return that.eProxyURI()==null;
                    }
                    String thisString = eProxyURI().toString();
                    String thatString = that.eProxyURI() == null ? null : that.eProxyURI().toString();
                    return thisString.equals(thatString);
                }
                else if (object instanceof String) {
                    String thisString = eProxyURI().toString();
                    return thisString.equals(object);
                }
                return super.equals(object);
            }

        };
        de.eSetClass(EcorePackage.eINSTANCE.getEObject());
        de.eSetProxyURI(URI.createURI(value));
        return de;
    }
}