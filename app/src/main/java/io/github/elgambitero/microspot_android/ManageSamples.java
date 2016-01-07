package io.github.elgambitero.microspot_android;


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.util.List;

import static android.support.v4.app.ActivityCompat.startActivity;

/**
 * Created by elgambitero on 30/12/15.
 */
public class ManageSamples extends AppCompatActivity implements ListView.OnItemClickListener,
        View.OnClickListener{

    String draweroptions[] = {"Manage Presets", "Calibrate", "Settings"};
    DrawerLayout drawer;
    RecyclerView sampleList;
    ListView drawerList;
    Toolbar manage_toolbar;
    ActionBarDrawerToggle drawerToggle;
    FloatingActionButton newScanFab;
    ManagerFabBehavior managerFabBehavior;
    CoordinatorLayout coordinatorLayout;
    SampleAdapter sampleAdapter;
    List<Sample> samples;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_samples);

        initStatusBar();
        initLayout();
        initToolbar();
        initFab();
        setAdapters();



    }

    private void initStatusBar(){

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));

    }

    private void initFab(){
        newScanFab = (FloatingActionButton)findViewById(R.id.newScanFab);
        newScanFab.setBackgroundTintList(getResources().getColorStateList(R.color.fab));
        newScanFab.setOnClickListener(this);
        managerFabBehavior = new ManagerFabBehavior(this,null);

        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams)
                newScanFab.getLayoutParams();
        p.setBehavior(managerFabBehavior);
        newScanFab.setLayoutParams(p);
    }

    private void initToolbar(){
        manage_toolbar = (Toolbar) findViewById(R.id.managetoolbar);
        setToolbarFunctions();
    }

    private void initLayout() {

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.manage_coordinator);
        drawer = (DrawerLayout) findViewById(R.id.manager_drawer);
        sampleList = (RecyclerView) findViewById(R.id.sample_list);
        drawerList = (ListView) findViewById(R.id.drawer_list);



    }

    private void setAdapters() {
        drawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, draweroptions));
        drawerList.setOnItemClickListener(this);

        samples = Sample.createSamplesList(this);
        // Create adapter passing in the sample user data
        sampleAdapter = new SampleAdapter(samples,this);
        // Attach the adapter to the recyclerview to populate items
        sampleList.setAdapter(sampleAdapter);
        // Set layout manager to position the items
        sampleList.setLayoutManager(new LinearLayoutManager(this));
        // That's all!

    }

    private void setToolbarFunctions(){
        setSupportActionBar(manage_toolbar);

        drawerToggle = new ActionBarDrawerToggle(this, drawer,
                manage_toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {

            // Called when a drawer has settled in a completely closed state.
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(R.string.managesamples_title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            // Called when a drawer has settled in a completely open state.
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle("Choose");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        drawer.setDrawerListener(drawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerToggle.setDrawerIndicatorEnabled(false);

        // mDrawerToggle.setHomeAsUpIndicator(R.drawable.menu_icon);
        manage_toolbar.setNavigationIcon(R.drawable.ic_navigation_drawer);

        manage_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.openDrawer(Gravity.LEFT);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        switch (parent.getId()) {
            case R.id.drawer_list:
                switch (position) {
                    case 1:
                        try{
                            Class next = Class.forName(getPackageName()+".Calibrate");
                            Intent i = new Intent(ManageSamples.this,next);
                            startActivity(i);
                        }catch(ClassNotFoundException e){
                            e.printStackTrace();
                        }
                        break;
                    default:
                        Snackbar.make(v, "UNIMPLEMENTED FEATURE",
                                Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        break;
                }
                break;

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_manage_samples_drawer,menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.newScanFab:
                Intent i = new Intent(getPackageName()+".NEWSCAN");
                startActivity(i);
                break;
        }
    }


}
