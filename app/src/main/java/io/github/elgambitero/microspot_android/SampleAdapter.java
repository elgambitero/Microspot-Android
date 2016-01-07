package io.github.elgambitero.microspot_android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;


/**
 * Adapted by elgambitero on 05/01/16.
 */
public class SampleAdapter extends RecyclerView.Adapter<SampleAdapter.ViewHolder>{

    private Context _context;

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView sampleTitle;
        private List<Sample> _samples;
        private Context mContext;
        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView, List<Sample> samples,Context context) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            sampleTitle = (TextView) itemView.findViewById(R.id.sampleTitle);
            itemView.setOnClickListener(this);
            _samples=samples;
            mContext=context;
        }

        @Override
        public void onClick(View v) {

            int position = getAdapterPosition();
            Sample sample = _samples.get(position);
            try {
                sample.getScanInfo(mContext);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Bundle scansSpecs = new Bundle();
            scansSpecs.putString("PatientID",sample.getId());
            scansSpecs.putString("annotations", sample.getAnno());
            Intent intento = new Intent("io.github.elgambitero.microspot_android.DETAILS");
            if(intento==null){

            Snackbar.make(v, "THIS LIST IS A PLACEHOLDER",
                    Snackbar.LENGTH_LONG).setAction("Action", null).show();

            }else{
                intento.putExtras(scansSpecs);
                mContext.startActivity(intento);
            }

        }


    }

    private List<Sample> _samples;

    public SampleAdapter(List<Sample> samples,Context context) {
        _samples = samples;
        _context=context;
    }

    @Override
    public SampleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View sampleView = inflater.inflate(R.layout.sample_list_item, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(sampleView, _samples, _context);
        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(SampleAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        Sample sample = _samples.get(position);

        // Set item views based on the data model
        TextView textView = viewHolder.sampleTitle;
        textView.setText(sample.getTitle());


    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return _samples.size();
    }

}
