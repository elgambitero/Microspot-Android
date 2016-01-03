package io.github.elgambitero.microspot_android;


import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Created by elgambitero on 30/12/15.
 */
public class ManageSamples extends AppCompatActivity implements ListView.OnItemClickListener, View.OnClickListener{

    String unimplementedlist[] = {"EX3324", "EX2204", "EX5345", "EX6543", "EX5346", "EX9877", "EX6336",
            "EX4652", "EX6325", "EX2346", "EX3632"};
    String draweroptions[] = {"Manage Presets", "Calibrate", "Settings"};
    DrawerLayout drawer;
    ListView sampleList, drawerList;
    Toolbar manage_toolbar;
    ActionBarDrawerToggle drawerToggle;
    FloatingActionButton newScanFab;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_samples);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        initializeVars();
        setAdapters();
        setToolbarFunctions();
    }

    private void initializeVars() {

        drawer = (DrawerLayout) findViewById(R.id.manager_drawer);
        sampleList = (ListView) findViewById(R.id.sample_list);
        drawerList = (ListView) findViewById(R.id.drawer_list);
        manage_toolbar = (Toolbar) findViewById(R.id.managetoolbar);
        newScanFab = (FloatingActionButton)findViewById(R.id.newScanFab);
        newScanFab.setBackgroundTintList(getResources().getColorStateList(R.color.fab));
        newScanFab.setOnClickListener(this);
    }

    private void setAdapters() {
        drawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, draweroptions));
        drawerList.setOnItemClickListener(this);
        sampleList.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_expandable_list_item_1, unimplementedlist));
        sampleList.setOnItemClickListener(this);


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
            case R.id.sample_list:
                switch (position) {
                    default:
                        Snackbar.make(v, "THIS LIST IS A PLACEHOLDER",
                                Snackbar.LENGTH_LONG).setAction("Action", null).show();
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
