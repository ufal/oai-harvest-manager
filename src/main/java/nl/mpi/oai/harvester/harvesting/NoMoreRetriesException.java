package nl.mpi.oai.harvester.harvesting;

public final class NoMoreRetriesException extends RuntimeException {
    public NoMoreRetriesException(String message){
        super(message);
    }
}
