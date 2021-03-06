package org.applab.digitizingdata;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.applab.digitizingdata.R;
import org.applab.digitizingdata.domain.model.Meeting;
import org.applab.digitizingdata.domain.model.MeetingLoanIssued;
import org.applab.digitizingdata.helpers.LoanRepaymentHistoryArrayAdapter;
import org.applab.digitizingdata.helpers.LoansIssuedHistoryArrayAdapter;
import org.applab.digitizingdata.helpers.MemberLoanIssueRecord;
import org.applab.digitizingdata.helpers.MemberLoanRepaymentRecord;
import org.applab.digitizingdata.helpers.Utils;
import org.applab.digitizingdata.repo.MeetingLoanIssuedRepo;
import org.applab.digitizingdata.repo.MeetingLoanRepaymentRepo;
import org.applab.digitizingdata.repo.MeetingRepo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Moses on 7/13/13.
 */
public class MemberLoansRepaidHistoryActivity extends SherlockListActivity {
    ActionBar actionBar;
    String meetingDate;
    int meetingId;
    int memberId;
    int targetCycleId = 0;
    Meeting targetMeeting = null;
    MeetingRepo meetingRepo = null;
    MeetingLoanIssuedRepo loanIssuedRepo = null;
    MeetingLoanRepaymentRepo loansRepaidRepo = null;
    ArrayList<MemberLoanRepaymentRecord> loanRepayments;
    MeetingLoanIssued recentLoan = null;

    //Flags for Edit Operation
    boolean isEditOperation = false;
    //This is the repayment that is being edited
    MemberLoanRepaymentRecord repaymentBeingEdited = null;

    //Fields for Rollover calculation
    double interestRate = 0.0;
    EditText editTextInterestRate;
    TextView txtRolloverAmount;
    TextView txtLoanBalance;
    double theCurLoanBalanceAmount = 0.0;
    double theCurLoanRepayAmount = 0.0;

    //Date stuff
    TextView txtDateDue;
    TextView viewClicked;
    public static final int Date_dialog_id = 1;
    // date and time
    private int mYear;
    private int mMonth;
    private int mDay;
    String dateString;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // BEGIN_INCLUDE (inflate_set_custom_view)
        // Inflate a "Done/Cancel" custom action bar view.
        final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(R.layout.actionbar_custom_view_done_cancel, null);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(recentLoan == null) {
                            //Utils.createAlertDialogOk(MemberLoansRepaidHistoryActivity.this, "Repayment","The member does not have an outstanding loan.", Utils.MSGBOX_ICON_EXCLAMATION).show();
                            finish();
                        }
                        else if(saveMemberLoanRepayment()) {
                            Toast.makeText(MemberLoansRepaidHistoryActivity.this, "Loan Repayment entered successfully", Toast.LENGTH_LONG).show();
                            Intent i = new Intent(getApplicationContext(), MeetingActivity.class);
                            i.putExtra("_tabToSelect","loansRepaid");
                            i.putExtra("_meetingDate",meetingDate);
                            i.putExtra("_meetingId", meetingId);
                            startActivity(i);
                            finish();
                        }

                    }
                });
        customActionBarView.findViewById(R.id.actionbar_cancel).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(getApplicationContext(), MeetingActivity.class);
                        i.putExtra("_tabToSelect","loansRepaid");
                        i.putExtra("_meetingDate",meetingDate);
                        i.putExtra("_meetingId", meetingId);
                        startActivity(i);
                        finish();
                    }
                });


        actionBar = getSupportActionBar();
        actionBar.setTitle("Repayments");
        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView,
                new ActionBar.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        // END_INCLUDE (inflate_set_custom_view)


        setContentView(R.layout.activity_member_loans_repaid_history);

        TextView lblMeetingDate = (TextView)findViewById(R.id.lblMLRepayHMeetingDate);
        meetingDate = getIntent().getStringExtra("_meetingDate");
        lblMeetingDate.setText(meetingDate);

        TextView lblFullNames = (TextView)findViewById(R.id.lblMLRepayHFullNames);
        String fullNames = getIntent().getStringExtra("_names");
        lblFullNames.setText(fullNames);

        if(getIntent().hasExtra("_meetingId")) {
            meetingId = getIntent().getIntExtra("_meetingId",0);
        }

        if(getIntent().hasExtra("_memberId")) {
            memberId = getIntent().getIntExtra("_memberId",0);
        }

        meetingRepo = new MeetingRepo(MemberLoansRepaidHistoryActivity.this);
        targetMeeting = meetingRepo.getMeetingById(meetingId);
        loanIssuedRepo = new MeetingLoanIssuedRepo(MemberLoansRepaidHistoryActivity.this);
        loansRepaidRepo = new MeetingLoanRepaymentRepo(MemberLoansRepaidHistoryActivity.this);

        //Determine whether this is an edit operation on an existing Loan Repayment
        repaymentBeingEdited = loansRepaidRepo.getLoansRepaymentByMemberInMeeting(meetingId,memberId);
        if(null != repaymentBeingEdited) {
            //Flag that this is an edit operation
            isEditOperation = true;
        }

        //Get Loan Number of currently running loan
        TextView lblLoanNo = (TextView)findViewById(R.id.lblMLRepayHLoanNo);
        TextView txtLoanNumber = (TextView)findViewById(R.id.txtMLRepayHLoanNo);
        TextView txtLoanAmountFld = (TextView)findViewById(R.id.txtMLRepayHAmount);
        TextView txtComment = (TextView)findViewById(R.id.txtMLRepayHComment);
        TextView txtBalance = (TextView)findViewById(R.id.txtMLRepayHBalance);
        TextView txtNewInterest = (TextView)findViewById(R.id.txtMLRepayHInterest);
        TextView txtTotal = (TextView)findViewById(R.id.txtMLRepayHTotal);
        TextView txtNewDateDue = (TextView)findViewById(R.id.txtMLRepayHDateDue);


        recentLoan = loanIssuedRepo.getMostRecentLoanIssuedToMember(memberId);
        if(null != recentLoan) {
            txtLoanNumber.setText(String.format("%d",recentLoan.getLoanNo()));

            //Now in case this is an edit operation populate the fields with the Repayment being edited
            if(null != repaymentBeingEdited && isEditOperation) {
                //populate the fields
                txtLoanAmountFld.setText(String.format("%.0f",repaymentBeingEdited.getAmount()));
                txtComment.setText(repaymentBeingEdited.getComments());
                //txtLoanNumber.setText(String.format("%d", repaymentBeingEdited.getLoanNo()));

                //Add the rest of the fields
                txtNewDateDue.setText(Utils.formatDate(repaymentBeingEdited.getNextDateDue()));
                txtBalance.setText(String.format("%.0f",repaymentBeingEdited.getBalanceAfter()));
                txtNewInterest.setText(String.format("%.0f",repaymentBeingEdited.getInterestAmount()));
                txtTotal.setText(String.format("%.0f",repaymentBeingEdited.getRolloverAmount()));
            }
        }
        else {
            txtLoanNumber.setText(null);

            //Show that Member has No Loan
            lblLoanNo.setText("Member does not have an outstanding loan.");

            //Remove the widgets for capturing Loans
            LinearLayout parent = (LinearLayout)lblLoanNo.getParent();

            //Remove LoanNo
            LinearLayout frmLoanNo = (LinearLayout)findViewById(R.id.frmMLRepayHLoanNo);
            parent.removeView(frmLoanNo);

            //Remove Amount
            TextView lblAmount = (TextView)findViewById(R.id.lblMLRepayHAmount);
            parent.removeView(lblAmount);
            LinearLayout frmAmount = (LinearLayout)findViewById(R.id.frmMLRepayHAmount);
            parent.removeView(frmAmount);

            //Remove Balance
            TextView lblBalance = (TextView)findViewById(R.id.lblMLRepayHBalance);
            parent.removeView(lblBalance);
            LinearLayout frmBalance = (LinearLayout)findViewById(R.id.frmMLRepayHBalance);
            parent.removeView(frmBalance);

            //Remove Interest
            TextView lblInterest = (TextView)findViewById(R.id.lblMLRepayHInterest);
            parent.removeView(lblInterest);
            LinearLayout frmInterest = (LinearLayout)findViewById(R.id.frmMLRepayHInterest);
            parent.removeView(frmInterest);

            //Remove Interest
            TextView lblTotal = (TextView)findViewById(R.id.lblMLRepayHTotal);
            parent.removeView(lblTotal);
            LinearLayout frmTotal = (LinearLayout)findViewById(R.id.frmMLRepayHTotal);
            parent.removeView(frmTotal);

            //Remove Comment
            TextView lblComment = (TextView)findViewById(R.id.lblMLRepayHComment);
            parent.removeView(lblComment);
            parent.removeView(txtComment);

            //Remove Date Due
            TextView lblNewDateDue = (TextView)findViewById(R.id.lblMLRepayHDateDue);
            parent.removeView(lblNewDateDue);
            parent.removeView(txtNewDateDue);

        }

        //Handle the Date stuff only when the fields are visible
        if(null != recentLoan) {
            //Date stuff
            txtDateDue = (TextView)findViewById(R.id.txtMLRepayHDateDue);
            viewClicked = txtDateDue;

            //If it is not an edit operation then initialize the date. Otherwise, retain the date pulled from db
            if(!isEditOperation) {
                initializeDate();
            }

            //Set onClick Listeners to load the DateDialog for MeetingDate
            txtDateDue.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //I want the Event Handler to handle both startDate and endDate
                    viewClicked = (TextView)view;
                    DatePickerDialog datePickerDialog = new DatePickerDialog( MemberLoansRepaidHistoryActivity.this, mDateSetListener, mYear, mMonth, mDay);
                    //TODO: Enable this feature in API 11 and above
                    //datePickerDialog.getDatePicker().setMaxDate(new Date().getTime());
                    datePickerDialog.show();
                }
            });

            //Setup the Default Date. Not sure whether I should block this off when editing a loan repayment
            if(!isEditOperation) {

                //TODO: Set the default Date to be MeetingDate + 1Month, instead of using today's date
                final Calendar c = Calendar.getInstance();
                if(null != targetMeeting) {
                    c.setTime(targetMeeting.getMeetingDate());
                }
                c.add(Calendar.MONTH, 1);
                mYear = c.get(Calendar.YEAR);
                mMonth = c.get(Calendar.MONTH);
                mDay = c.get(Calendar.DAY_OF_MONTH);
                updateDisplay();
            }
            //end of date stuff
        }

        TextView txtOutstandingLoans = (TextView)findViewById(R.id.lblMLRepayHOutstandingLoans);

        double outstandingLoans = 0.0;
        if(targetMeeting != null && targetMeeting.getVslaCycle() != null) {
            targetCycleId = targetMeeting.getVslaCycle().getCycleId();
            outstandingLoans = loanIssuedRepo.getTotalOutstandingLoansByMemberInCycle(targetCycleId, memberId);
        }
        txtOutstandingLoans.setText(String.format("Total Balance: %,.0f UGX", outstandingLoans));

        //Populate the History
        populateLoanRepaymentHistory();

        //TODO: Check this
        if(null != recentLoan) {
            TextView txtLRAmount = (TextView)findViewById(R.id.txtMLRepayHAmount);
            txtLRAmount.requestFocus();
        }
        else {
            if(null != lblLoanNo) {
                lblLoanNo.setFocusable(true);
                lblLoanNo.requestFocus();
            }
        }

        //Handle the Auto-calculation of Rollover Amount. If recentLoan is NULL means fields are hidden
        if(null == recentLoan) {
            return;
        }
        //Handle the Loan Interest Computation
        editTextInterestRate = (EditText)findViewById(R.id.txtMLRepayHInterest);
        txtRolloverAmount = (TextView)findViewById(R.id.txtMLRepayHTotal);
        txtLoanBalance = (TextView)findViewById(R.id.txtMLRepayHBalance);

        //First get the Interest Rate for the Current Cycle
        if(targetMeeting != null && targetMeeting.getVslaCycle() != null) {
            interestRate = targetMeeting.getVslaCycle().getInterestRate();
        }

        EditText txtRepaymentAmount = (EditText)findViewById(R.id.txtMLRepayHAmount);
        txtRepaymentAmount.addTextChangedListener(new TextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Compute the Interest
                double theRepayAmount = 0.0;
                try {
                    if(s.toString().length() <= 0) {
                        return;
                    }
                    theRepayAmount = Double.parseDouble(s.toString());
                }
                catch(Exception ex) {
                    return;
                }

                //Compute the Balance
                if(isEditOperation && null != repaymentBeingEdited) {
                    theCurLoanBalanceAmount = repaymentBeingEdited.getBalanceBefore() - theRepayAmount;
                }
                else {
                    theCurLoanBalanceAmount = recentLoan.getLoanBalance() - theRepayAmount;
                }
                txtLoanBalance.setText(String.format("%,.0f",theCurLoanBalanceAmount));

                double interestAmount = (interestRate * 0.01 * theCurLoanBalanceAmount);
                editTextInterestRate.setText(String.format("%.0f",interestAmount));

                double rolloverAmount = theCurLoanBalanceAmount + interestAmount;
                txtRolloverAmount.setText(String.format("%,.0f",rolloverAmount));

                //Have this value redundantly stored for future use
                theCurLoanRepayAmount = theRepayAmount;
            }
        });

        //Now deal with Loan Interest Manual Changes
        EditText txtNewInterestAmount = (EditText)findViewById(R.id.txtMLRepayHInterest);
        txtNewInterestAmount.addTextChangedListener(new TextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Compute the Interest
                double theInterestAmount = 0.0;
                try {
                    if(s.toString().length() <= 0) {
                        return;
                    }
                    theInterestAmount = Double.parseDouble(s.toString());
                }
                catch(Exception ex) {
                    return;
                }

                double rolloverAmount = theInterestAmount + theCurLoanBalanceAmount;
                txtRolloverAmount.setText(String.format("%,.0f",rolloverAmount));
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.member_loans_repaid_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = new Intent(MemberLoansRepaidHistoryActivity.this, MeetingActivity.class);
                upIntent.putExtra("_tabToSelect","loansRepaid");
                upIntent.putExtra("_meetingDate",meetingDate);
                upIntent.putExtra("_meetingId", meetingId);

                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    TaskStackBuilder
                            .from(this)
                            .addNextIntent(new Intent(this, MainActivity.class))
                            .addNextIntent(upIntent).startActivities();
                    finish();
                } else {
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
            case R.id.mnuMLRepayHCancel:
                i = new Intent(MemberLoansRepaidHistoryActivity.this, MeetingActivity.class);
                i.putExtra("_tabToSelect","loansRepaid");
                i.putExtra("_meetingDate",meetingDate);
                i.putExtra("_meetingId", meetingId);
                startActivity(i);
                return true;
            case R.id.mnuMLRepayHSave:
                if(recentLoan == null) {
                    Utils.createAlertDialogOk(MemberLoansRepaidHistoryActivity.this, "Repayment","The member does not have an outstanding loan.", Utils.MSGBOX_ICON_EXCLAMATION).show();
                    return true;
                }
                if(saveMemberLoanRepayment()) {
                    i = new Intent(MemberLoansRepaidHistoryActivity.this, MeetingActivity.class);
                    i.putExtra("_tabToSelect","loansRepaid");
                    i.putExtra("_meetingDate",meetingDate);
                    i.putExtra("_meetingId", meetingId);
                    startActivity(i);
                }
                return true;
        }
        return true;
    }

    private void populateLoanRepaymentHistory() {
        if(loansRepaidRepo == null) {
            loansRepaidRepo = new MeetingLoanRepaymentRepo(MemberLoansRepaidHistoryActivity.this);
        }
        loanRepayments = loansRepaidRepo.getLoansRepaymentsByMemberInCycle(targetCycleId, memberId);

        if(loanRepayments == null) {
            loanRepayments = new ArrayList<MemberLoanRepaymentRecord>();
        }

        //Now get the data via the adapter
        LoanRepaymentHistoryArrayAdapter adapter = new LoanRepaymentHistoryArrayAdapter(MemberLoansRepaidHistoryActivity.this, loanRepayments);

        //Assign Adapter to ListView
        setListAdapter(adapter);

        //Hack to ensure all Items in the List View are visible
        Utils.setListViewHeightBasedOnChildren(getListView());
    }

    public boolean saveMemberLoanRepayment(){
        double theAmount = 0.0;

        try{
            if(recentLoan == null) {
                Utils.createAlertDialogOk(MemberLoansRepaidHistoryActivity.this, "Repayment","The member does not have an outstanding loan.", Utils.MSGBOX_ICON_EXCLAMATION).show();
                return false;
            }

            TextView txtLoanAmount = (TextView)findViewById(R.id.txtMLRepayHAmount);
            TextView txtComments = (TextView)findViewById(R.id.txtMLRepayHComment);
            TextView txtBalance = (TextView)findViewById(R.id.txtMLRepayHBalance);
            TextView txtInterest = (TextView)findViewById(R.id.txtMLRepayHInterest);
            TextView txtRollover = (TextView)findViewById(R.id.txtMLRepayHTotal);
            TextView txtNextDateDue = (TextView)findViewById(R.id.txtMLRepayHDateDue);

            String amount = txtLoanAmount.getText().toString().trim();
            if (amount.length() < 1) {
                Utils.createAlertDialogOk(MemberLoansRepaidHistoryActivity.this, "Repayment", "The Loan Amount is required.", Utils.MSGBOX_ICON_EXCLAMATION).show();
                txtLoanAmount.requestFocus();
                return false;
            }
            else {
                theAmount = Double.parseDouble(amount);
                if (theAmount < 0.00) {
                    Utils.createAlertDialogOk(MemberLoansRepaidHistoryActivity.this, "Repayment","The Loan Amount is invalid.", Utils.MSGBOX_ICON_EXCLAMATION).show();
                    txtLoanAmount.requestFocus();
                    return false;
                }
            }

            double newBalance = 0.0;
            if(isEditOperation && null != repaymentBeingEdited) {
                newBalance = repaymentBeingEdited.getBalanceBefore() - theAmount;
            }
            else {
                newBalance = recentLoan.getLoanBalance() - theAmount;
            }
            double theInterest = 0.0;

            String interest = txtInterest.getText().toString().trim();
            if (interest.length() < 1) {
                theInterest = 0.0;
            }
            else {
                theInterest = Double.parseDouble(interest);
                if (theInterest < 0.00) {
                    Utils.createAlertDialogOk(MemberLoansRepaidHistoryActivity.this, "Repayment","The Interest Amount is invalid.", Utils.MSGBOX_ICON_EXCLAMATION).show();
                    txtInterest.requestFocus();
                    return false;
                }
            }

            double theRollover = newBalance + theInterest;

            //Next Due Date
            Calendar cal = Calendar.getInstance();
            Date today = cal.getTime();

            Calendar calNext = Calendar.getInstance();
            calNext.add(Calendar.MONTH,1);
            Date theDateDue = calNext.getTime();

            //Check the date against the Meeting Date, not calendar date
            String nextDateDue = txtNextDateDue.getText().toString().trim();
            Date dtNextDateDue = Utils.getDateFromString(nextDateDue,Utils.DATE_FIELD_FORMAT);
            if (dtNextDateDue.before(targetMeeting.getMeetingDate())) {
                Utils.createAlertDialogOk(MemberLoansRepaidHistoryActivity.this, "Loan Issue","The due date has to be a future date.", Utils.MSGBOX_ICON_EXCLAMATION).show();
                txtNextDateDue.setFocusable(true);
                txtDateDue.requestFocus();
                return false;
            }
            else {
                theDateDue = dtNextDateDue;
            }

            String comments = txtComments.getText().toString().trim();

            //Now Save the data
            if(null == loansRepaidRepo){
                loansRepaidRepo = new MeetingLoanRepaymentRepo(MemberLoansRepaidHistoryActivity.this);
            }

            //retrieve the LoanId and LoanNo of the most recent uncleared loan
            int recentLoanId = 0;
            double balanceBefore = 0.0;
            Date dtLastDateDue = null;
            if(null != recentLoan) {
                recentLoanId = recentLoan.getLoanId();

                //If this is an edit then get the values from the repayment being edited
                if(isEditOperation && null != repaymentBeingEdited) {
                    balanceBefore = repaymentBeingEdited.getBalanceBefore();
                    dtLastDateDue = repaymentBeingEdited.getLastDateDue();
                }
                else {
                    balanceBefore = recentLoan.getLoanBalance();
                    //Last Date Due for Transaction Tracking purposes. Get it from the recent Loan
                    dtLastDateDue = recentLoan.getDateDue();
                }
            }
            else {
                //check again: Do not save repayment if there is no existing loan
                Utils.createAlertDialogOk(MemberLoansRepaidHistoryActivity.this, "Repayment","The member has no Outstanding Loan.", Utils.MSGBOX_ICON_EXCLAMATION).show();
                return false;
            }

            //Check Over-Payments
            if(theAmount > balanceBefore) {
                Utils.createAlertDialogOk(MemberLoansRepaidHistoryActivity.this, "Repayment","The repayment amount is more than the remaining loan balance.", Utils.MSGBOX_ICON_EXCLAMATION).show();
                return false;
            }

            //If Amount is Zero, then ensure that the date is due before doing a rollover
            if(theAmount == 0) {
                if(targetMeeting.getMeetingDate().before(recentLoan.getDateDue())) {
                    Utils.createAlertDialogOk(MemberLoansRepaidHistoryActivity.this, "Repayment","The repayment amount of zero (0 UGX) is not allowed when the loan is not yet due.", Utils.MSGBOX_ICON_EXCLAMATION).show();
                    return false;
                }
            }

            //If it is an editing of existing loan repayment, first undo the changes of the former one
            boolean undoSucceeded = false;
            if(isEditOperation && repaymentBeingEdited != null) {
                //Post a Reversal or just edit the figures
                undoSucceeded = loanIssuedRepo.updateMemberLoanBalances(recentLoan.getLoanId(),recentLoan.getTotalRepaid() - repaymentBeingEdited.getAmount(), repaymentBeingEdited.getBalanceBefore(), repaymentBeingEdited.getLastDateDue());
            }

            //If it was an edit operation and undo changes failed, then exit
            if(isEditOperation && !undoSucceeded ) {
                return false;
            }

            //Otherwise, proceed
            //saveMemberLoanRepayment(int meetingId, int memberId, int loanId, double amount, double balanceBefore, String comments, double balanceAfter,double interestAmount, double rolloverAmount, Date lastDateDue, Date nextDateDue)//
            boolean saveRepayment = loansRepaidRepo.saveMemberLoanRepayment(meetingId, memberId, recentLoanId, theAmount, balanceBefore, comments,newBalance,theInterest,theRollover, dtLastDateDue, dtNextDateDue);
            if(saveRepayment) {
                //Also update the balances
                if (loanIssuedRepo == null) {
                    loanIssuedRepo = new MeetingLoanIssuedRepo(MemberLoansRepaidHistoryActivity.this);
                }

                //TODO: Decide whether to update the Interest Paid also: and whether it will be Cummulative Interest or Just current Interest
                                        //updateMemberLoanBalances(int loanId, double totalRepaid, double balance, Date newDateDue)
                return loanIssuedRepo.updateMemberLoanBalances(recentLoan.getLoanId(),recentLoan.getTotalRepaid() + theAmount, theRollover, theDateDue);

            }
            else {
                //Saving failed
                return false;
            }
        }
        catch(Exception ex) {
            Log.e("MemberLoansRepaidHistory.saveMemberLoanRepayment", ex.getMessage());
            return false;
        }
    }

    //DATE
    //Event that is raised when the date has been set
    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;
            updateDisplay();
        }
    };

    @Override
    @Deprecated
    protected void onPrepareDialog(int id, Dialog dialog) {
        // TODO Auto-generated method stub
        super.onPrepareDialog(id, dialog);
        ((DatePickerDialog) dialog).updateDate(mYear, mMonth, mDay);
    }

    //Displays the selected Date in the TextView
    private void updateDisplay() {
        if(viewClicked != null) {
            dateString = (new StringBuilder()
                    // Month is 0 based so add 1
                    .append(mDay)
                    .append("-")
                    .append(Utils.getMonthNameAbbrev(mMonth + 1))
                    .append("-")
                    .append(mYear)).toString();
            viewClicked.setText(dateString);
        }
        else {
            //Not sure yet on what to do
        }
    }

    private void initializeDate(){
        if(viewClicked != null) {
            Calendar c = Calendar.getInstance();
            if(null != targetMeeting) {
                c.setTime(targetMeeting.getMeetingDate());
            }
            c.add(Calendar.MONTH,1);
            dateString = Utils.formatDate(c.getTime());
            viewClicked.setText(dateString);
        }
    }
}
