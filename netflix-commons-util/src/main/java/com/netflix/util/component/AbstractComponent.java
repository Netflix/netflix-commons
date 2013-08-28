package com.netflix.util.component;

public abstract class AbstractComponent implements Component {

    enum State {
        CONSTRUCTED,
        STARTING,
        STARTED,
        STOP,
        PAUSE,
        RESUME, 
        FAILED
    }
    
    private State currentState = State.CONSTRUCTED;
    
    protected abstract void internalStart() throws Exception;
    protected abstract void internalStop() throws Exception;
    protected abstract void internalPause() throws Exception;
    protected abstract void internalResume() throws Exception;
    
    @Override
    public final synchronized void start() throws Exception {
        changeState(State.STARTING, State.CONSTRUCTED);
        internalStart();
        changeState(State.STARTED,  State.STARTING);
    }

    @Override
    public final synchronized void stop() throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public final synchronized void pause() throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public final synchronized void resume() throws Exception {
        // TODO Auto-generated method stub
        
    }

    private void changeState(State newState, State expectedState) throws Exception {
        if (currentState == expectedState) {
            currentState = newState;
        }
        else {
            throw new IllegalStateException(String.format("Cannot transition from '%s' to '%s'", expectedState.name(), newState.name()));
        }
    }
}
