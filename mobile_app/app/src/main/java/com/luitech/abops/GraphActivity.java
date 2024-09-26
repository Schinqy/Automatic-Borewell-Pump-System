package com.luitech.abops;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class GraphActivity extends AppCompatActivity {
    private String graph_type;
    private LineChart lineChart;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_graph);
        lineChart = findViewById(R.id.lineChart);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        Intent intent = getIntent();
        graph_type = intent.getStringExtra("graph_type");
        populateGraph();

        switch (graph_type) {
            case "water":
                toolbar.setTitle("Water Level Graph");
                break;

            default:
                toolbar.setTitle("Flow Rate Graph");
                break;
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void populateGraph() {
        // Create a list of data entries
        ArrayList<Entry> entries = new ArrayList<>();

        // Example data points (you can replace these with your actual data)
        entries.add(new Entry(0, 2));
        entries.add(new Entry(1, 4));
        entries.add(new Entry(2, 1));
        entries.add(new Entry(3, 5));
        entries.add(new Entry(4, 3));

        // Create a LineDataSet and set its properties
        LineDataSet lineDataSet = new LineDataSet(entries, "Example Data");
        lineDataSet.setColor(ColorTemplate.COLORFUL_COLORS[0]); // Color for the line
        lineDataSet.setValueTextColor(getResources().getColor(R.color.white)); // Text color for values
        lineDataSet.setLineWidth(2f); // Line width
        lineDataSet.setCircleColor(getResources().getColor(R.color.white)); // Circle color for data points
        lineDataSet.setCircleRadius(5f); // Circle radius
        lineDataSet.setDrawValues(true); // Show values above points

        // Set the LineDataSet to the LineChart
        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);

        // Customize the X-axis and Y-axis if needed
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Set X-axis position
        xAxis.setGranularity(1f); // Control X-axis increments
        xAxis.setLabelCount(5); // Number of X-axis labels

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setGranularity(1f); // Control Y-axis increments

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false); // Disable the right Y-axis

        // Refresh the graph
        lineChart.invalidate(); // Refresh the chart with new data
    }
}