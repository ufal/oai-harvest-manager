package nl.mpi.oai.harvester.harvesting.scenarios;

import nl.mpi.oai.harvester.Provider;
import nl.mpi.oai.harvester.StaticProvider;
import nl.mpi.oai.harvester.action.ActionSequence;

public class ScenarioFactory {
    public static Scenario getScenario(Provider provider, ActionSequence actionSequence){
        Scenario scenario;
        if("ListIdentifiers".equals(provider.getScenario())){
            scenario = new IndirectScenario(provider, actionSequence);
        }else{
            scenario = new DirectScenario(provider, actionSequence);
        }
        if(provider instanceof StaticProvider){
            scenario = new StaticScenario(scenario);
        }
        if(provider.shouldResume()){
            scenario = new ResumeScenario(scenario);
        }
        return scenario;

    }
}
