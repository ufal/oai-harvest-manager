package nl.mpi.oai.harvester.action;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import nl.mpi.oai.harvester.metadata.Metadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class StaticParamTransformAction extends TransformAction {
    private static final Logger logger = LogManager.getLogger(StaticParamTransformAction.class);

    private String currName = "";

    public StaticParamTransformAction(Node conf, String xsltFile, Path cacheDir, int maxJobs) throws FileNotFoundException, TransformerConfigurationException, MalformedURLException, SaxonApiException {
        super(conf, xsltFile, cacheDir, maxJobs);
    }

    StaticParamTransformAction(Node conf, String xsltFile,Path cacheDir, int maxJobs, AtomicInteger counter) throws FileNotFoundException, SaxonApiException {
        super(conf, xsltFile, cacheDir, maxJobs, counter);
    }

    @Override
    public boolean perform(List<Metadata> records){
        final String pName = records.get(0).getOrigin().getName();
        if(!pName.equals(currName)){
            currName = pName;
            try {
                compile();
            } catch (FileNotFoundException | SaxonApiException e) {
                logger.error(e);
                return false;
            }
        }
        return super.perform(records);
    }

    private void compile() throws FileNotFoundException, SaxonApiException {
        Source xslSource = getSourceFromFile();
        xsltCompiler.setParameter(new QName("static_provider_name"), new XdmAtomicValue(currName));
        final long compileStart = System.currentTimeMillis();
        executable = xsltCompiler.compile(xslSource);
        final long compileEnd = System.currentTimeMillis();
        if(logger.isDebugEnabled()){
            logger.debug(String.format("The compilation of %s took %s ms - recompiling", xsltFile,
                    compileEnd - compileStart));
        }
    }

    @Override
    public String toString() {
        return "transform using " + xsltFile;
    }

    // Transform actions differ if and only if the XSLT files differ.
    @Override
    public int hashCode() {
        return Objects.hash("static", xsltFile);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof StaticParamTransformAction) {
            StaticParamTransformAction t = (StaticParamTransformAction) o;
            return xsltFile.equals(t.xsltFile);
        }
        return false;
    }

    @Override
    public Action clone() {
        try {
            // This is a deep copy. The new object has its own Transform object.
            return new StaticParamTransformAction(config, xsltFile,cacheDir, maxJobs, runningTransformationsCounter);
        } catch (FileNotFoundException | SaxonApiException ex) {
            logger.error(ex);
        }
        return null;
    }
}
