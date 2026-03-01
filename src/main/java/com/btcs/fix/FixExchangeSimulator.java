package com.btcs.fix;

import quickfix.*;
import quickfix.fix44.*;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.FieldNotFound;
import quickfix.MessageCracker;
import quickfix.Application;

public class FixExchangeSimulator extends MessageCracker implements Application {
	
	private HandleIncomingMsg handler;
	//private MatchingEngine matcher;
	
	public FixExchangeSimulator() {

	// Create repository ONCE
	OrderRepository orderRepository = OrderRepository.getInstance();
	Executions executions = new Executions();
	OrderValidations validations = new OrderValidations();
	OrderBookManager orderBookManager = OrderBookManager.getInstance();
	StopOrderManager stopOrderManager = new StopOrderManager(orderBookManager, executions, orderRepository);
	MatchingEngine matchingEngine = new MatchingEngine(executions, stopOrderManager);
	
	// Inject repository into handler
	this.handler = new HandleIncomingMsg(orderRepository,executions,validations,orderBookManager,matchingEngine, stopOrderManager);
	//this.matcher = new MatchingEngine(executions);
	}
	

    @Override
    public void onCreate(SessionID sessionId) {
        System.out.println("Session created: " + sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("Client logged on: " + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("Client logged out: " + sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
    }

    @Override
    public void toApp(Message message, SessionID sessionId) {
        //System.out.println("Sending: " + message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {

        crack(message, sessionId);
    }

    public void onMessage(NewOrderSingle order, SessionID sessionId)
            throws FieldNotFound {

        handler.handleNewOrder(order, sessionId);
    }

    public void onMessage(OrderCancelRequest cancel, SessionID sessionId)
            throws FieldNotFound {

        handler.handleCancel(cancel, sessionId);
    }

    public void onMessage(OrderCancelReplaceRequest replace, SessionID sessionId)
            throws FieldNotFound {

        handler.handleReplace(replace, sessionId);
    }
}
