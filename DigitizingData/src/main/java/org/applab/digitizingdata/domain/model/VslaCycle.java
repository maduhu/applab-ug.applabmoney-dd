package org.applab.digitizingdata.domain.model;

import java.util.Date;

/**
 * Created by Moses on 6/25/13.
 */
public class VslaCycle {

    private int cycleId;
    private String cycleCode;
    private Date startDate;
    private Date endDate;
    private double sharePrice;
    private double maxSharesQty;
    private double maxStartShare;
    private double interestRate;
    private boolean isActive;
    private boolean isEnded;
    private Date dateEnded;
    private double sharedAmount;

    public VslaCycle(){

    }

    public VslaCycle(int cycleId, String cycleCode, Date startDate, Date endDate, double sharePrice, double maxSharesQty, double maxStartShare, double interestRate){
        this.cycleId = cycleId;
        this.cycleCode = cycleCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.sharePrice = sharePrice;
        this.maxSharesQty = maxSharesQty;
        this.maxStartShare = maxStartShare;
        this.interestRate = interestRate;
    }

    public VslaCycle(int cycleId) {
        this(cycleId,null,null,null,0.0,0.0,0.0,0.0);
    }

    public VslaCycle(int cycleId,Date startDate, Date endDate) {
        this(cycleId,null, startDate, endDate,0.0,0.0,0.0,0.0);
    }

    public int getCycleId() {
        return cycleId;
    }

    public void setCycleId(int cycleId) {
        this.cycleId = cycleId;
    }

    public String getCycleCode() {
        return cycleCode;
    }

    public void setCycleCode(String cycleCode) {
        this.cycleCode = cycleCode;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public double getSharePrice() {
        return sharePrice;
    }

    public void setSharePrice(double sharePrice) {
        this.sharePrice = sharePrice;
    }

    public double getMaxSharesQty() {
        return maxSharesQty;
    }

    public void setMaxSharesQty(double maxSharesQty) {
        this.maxSharesQty = maxSharesQty;
    }

    public double getMaxStartShare() {
        return maxStartShare;
    }

    public void setMaxStartShare(double maxStartShare) {
        this.maxStartShare = maxStartShare;
    }

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        isActive = true;
    }

    public void deactivate() {
        isActive = false;
    }

    public boolean isEnded() {
        return isEnded;
    }

    public void end(Date dateEnded) {
        end(dateEnded,0.0);
    }

    public void end(Date dateEnded, double sharedAmount) {
        isEnded = true;
        this.dateEnded = dateEnded;
        this.sharedAmount = sharedAmount;

        //Not very sure I need this
        this.deactivate();
    }

    public Date getDateEnded() {
        return dateEnded;
    }

    public double getSharedAmount() {
        return sharedAmount;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

}
