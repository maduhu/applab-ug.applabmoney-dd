package org.applab.digitizingdata;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;

import org.applab.digitizingdata.domain.model.Meeting;
import org.applab.digitizingdata.domain.model.VslaCycle;
import org.applab.digitizingdata.fontutils.RobotoTextStyleExtractor;
import org.applab.digitizingdata.fontutils.TypefaceManager;
import org.applab.digitizingdata.fontutils.TypefaceTextView;
import org.applab.digitizingdata.helpers.Utils;
import org.applab.digitizingdata.repo.*;

import java.util.ArrayList;




public class MeetingSendDataFrag extends SherlockFragment {

    ActionBar actionBar = null;
    int numberOfPastUnsentMeetings = 0;
    private Meeting currentMeeting;
    private int numberOfMembers;
    private double totalSavingsInCurrentMeeting;
    private double totalLoansRepaidInCurrentMeeting;
    private double totalFinesInCurrentMeeting;
    private double totalLoansIssuedInCurrentMeeting;
    private int currentMeetingAttendance;
    int currentMeetingId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        setHasOptionsMenu(true);
        currentMeetingId = getSherlockActivity().getIntent().getIntExtra("_meetingId", 0);
        return inflater.inflate(R.layout.frag_meeting_send_data, container, false);

    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TypefaceManager.addTextStyleExtractor(RobotoTextStyleExtractor.getInstance());

        actionBar = getSherlockActivity().getSupportActionBar();
        String title = "Meeting";

        switch(Utils._meetingDataViewMode) {
            case VIEW_MODE_REVIEW:
                title = "Send Data";
                break;
            case VIEW_MODE_READ_ONLY:
                title = "Sent Data";
                break;
            default:
                title="Meeting";
                break;
        }
        actionBar.setTitle(title);
        refreshFragmentView();

    }

    private void refreshFragmentView() {

        loadFragmentInformation(currentMeetingId);
        Log.i("SendDataFrag", "number of unsent meetings is " + numberOfPastUnsentMeetings);
        if(numberOfPastUnsentMeetings == 0) {
            //Hide unrequired views
            LinearLayout layoutMSDUnsentPastMeetings = (LinearLayout) getSherlockActivity().findViewById(R.id.layoutMSDUnsentPastMeetings);
            layoutMSDUnsentPastMeetings.setVisibility(View.INVISIBLE);

        }
        populateCurrentMeetingSummary();
        TextView txtStatus = (TextView)getSherlockActivity().findViewById(R.id.lblMSDFragStatus);
        TextView txtInstructions = (TextView)getSherlockActivity().findViewById(R.id.lblMSDFragInstructions);
        if(isNetworkConnected(getSherlockActivity().getApplicationContext())) {
            txtStatus.setText("The data network is available.");
            txtInstructions.setText("You can send the meeting data now by tapping the Send button above.");
        }
        else {
            txtStatus.setText("The data network is not available.");
            txtInstructions.setText("Move to a place with data network to send the meeting data. You can send the data later by selecting Check & Send Data from the main menu.");
        }
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected());
    }

    /*counts the number of past unset meetings, and computes current meeting saved values */
    public void loadFragmentInformation(int currentMeetingId) {

       MeetingRepo meetingRepo = new MeetingRepo(this.getSherlockActivity().getBaseContext());
       ArrayList<Meeting> unsentMeetings = meetingRepo.getAllMeetingsByDataSentStatus(false);
       numberOfPastUnsentMeetings = unsentMeetings.size();

        //Get the current meeting
        currentMeeting = meetingRepo.getMeetingById(currentMeetingId);

        //Get total savings in current meeting
        MeetingSavingRepo meetingSavingRepo = new MeetingSavingRepo(getSherlockActivity().getBaseContext());
        totalSavingsInCurrentMeeting = meetingSavingRepo.getTotalSavingsInMeeting(currentMeeting.getMeetingId());

        
        MeetingLoanRepaymentRepo meetingLoanRepaymentRepo = new MeetingLoanRepaymentRepo(getSherlockActivity().getBaseContext());
        totalLoansRepaidInCurrentMeeting = meetingLoanRepaymentRepo.getTotalLoansRepaidInMeeting(currentMeeting.getMeetingId());


        //Get total fines in meeting
        MeetingFineRepo meetingFineRepo = new MeetingFineRepo(getSherlockActivity().getBaseContext());
        totalFinesInCurrentMeeting = meetingFineRepo.getTotalFinesInMeeting(currentMeeting.getMeetingId());
        
        //Get total loans in current meeting
        MeetingLoanIssuedRepo meetingLoanIssuedRepo = new MeetingLoanIssuedRepo(getSherlockActivity().getBaseContext());
        totalLoansIssuedInCurrentMeeting = meetingLoanIssuedRepo.getTotalLoansIssuedInMeeting(currentMeeting.getMeetingId());
        
        //Get attendance in current meeting
        MeetingAttendanceRepo meetingAttendanceRepo = new MeetingAttendanceRepo(getSherlockActivity().getBaseContext());
        currentMeetingAttendance = meetingAttendanceRepo.getAttendanceCountByMeetingId(currentMeeting.getMeetingId(), 1);

        //Get count of all members
        MemberRepo memberRepo = new MemberRepo(getSherlockActivity().getBaseContext());
        numberOfMembers = memberRepo.countMembers();



    }

    //Populates the summary for the current meeting
    public void populateCurrentMeetingSummary() {

        TextView lblMSDFragRollcall = (TextView) getSherlockActivity().findViewById(R.id.lblMSDFragRollcall);
        lblMSDFragRollcall.setText(String.format("Roll Call %d/%d", currentMeetingAttendance, numberOfMembers));

        TextView lblMSDFragSavings = (TextView) getSherlockActivity().findViewById(R.id.lblMSDFragSavings);
        lblMSDFragSavings.setText(String.format("Savings %,.0f UGX", totalSavingsInCurrentMeeting));

        TextView lblMSDFragLoanPayments = (TextView) getSherlockActivity().findViewById(R.id.lblMSDFragLoanPayments);
        lblMSDFragLoanPayments.setText(String.format("Loan Payments %,.0f UGX", totalLoansRepaidInCurrentMeeting));

        TextView lblMSDFragFines = (TextView) getSherlockActivity().findViewById(R.id.lblMSDFragFines);
        lblMSDFragFines.setText(String.format("Fines %,.0f UGX", totalFinesInCurrentMeeting));

        TextView lblMSDFragNewLoans = (TextView) getSherlockActivity().findViewById(R.id.lblMSDFragNewLoans);
        lblMSDFragNewLoans.setText(String.format("New Loans %,.0f UGX", totalLoansIssuedInCurrentMeeting));


    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        menu.clear();
        if(isNetworkConnected(getSherlockActivity().getApplicationContext())) {
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.meeting_send_data, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                return false;
        /**    case R.id.mnuSMDSend:
                return false;
            case R.id.mnuSMDCancel:
                return false; */
            case R.id.mnuMCBFSave:
                return false;
            case R.id.mnuMSDFSend:
                return false;
            default:
                return false;
        }
    }
}
