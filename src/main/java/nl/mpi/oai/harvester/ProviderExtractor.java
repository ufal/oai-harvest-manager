package nl.mpi.oai.harvester;

import org.eclipse.persistence.descriptors.ClassExtractor;
import org.eclipse.persistence.sessions.Record;
import org.eclipse.persistence.sessions.Session;

public class ProviderExtractor extends ClassExtractor {
    @Override
    public Class extractClassFromRow(Record record, Session session) {

        boolean isStatic = Boolean.parseBoolean((String)record.get("@static"));
        if (isStatic) {
            return StaticProvider.class;
        }
        return Provider.class;
    }
}
