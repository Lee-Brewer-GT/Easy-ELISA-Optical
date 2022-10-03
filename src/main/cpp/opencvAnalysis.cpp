

#include "opencvAnalysis.h"


//#include "opencvAnalysis.hpp"
#include <list>
#include <algorithm>
#include <vector>
using namespace cv;
using namespace std;

int     c[]={640,320,160,80,40,20,10};
//int size = 7;
bool sortcol( const vector<int>& v1,
              const vector<int>& v2 ) {
    return v1[0] < v2[0];
}


vector<int> opencvAnalysis::detect_biomarker(Mat image, int *size, int *replicates) // add argument for concentrations?
{



    int     radius = 25;
    //*replicates = 3; //and this
    int     threshval = 155;
    double  minVal, maxVal;
    Point   minLoc, maxLoc;
    //vector<double> greyscaleVals;
    //vector<Point> locations;
    //int output[7][2];  // now sort vals by points
    vector<vector<int>> outputs(*size);
    //list<int> concentrations (c,c+7);
    //list<int> locations = concentrations;
    int colom[] = {2,2,2,2,2,2,2,2,2,2};  //this sucks
    for(int i = 0; i < *size; i++)
    {
        /* Declaring the size of the column. */
        int col = colom[i];

        outputs[i] = vector<int>(col);
        for(int j = 0; j < col; j++)
        {
            outputs[i][j] = j + 1;
        }
    }
    // Number of rows;

    // Displaying the 2D vector after sorting



    Mat greyImage;
    cvtColor(image, greyImage, cv::COLOR_BGR2GRAY);
    Mat blurredImage;
    GaussianBlur(greyImage,blurredImage, Size(radius,radius),0); // see whether the gaussian blur actually changes anything


    //while loop:
    //starts with openCV function to run on image
    //
    // namedWindow( "Display window", CV_WINDOW_AUTOSIZE );// Create a window for display.
    // imshow( "Display window", blurredImage );
    //waitkey(0);
    int count = 0;
    minVal = 0.00;
    while(minVal < threshval)
    {
        minMaxLoc(blurredImage, &minVal, &maxVal, &minLoc, &maxLoc);
        circle(blurredImage, minLoc,radius, Scalar(255,255,255),CV_FILLED, 8,0);
        // minMaxLoc

        //matrix of the minVals to graph
        int X = minLoc.x;
        //double x = (double)X;
        //  int Y = minLoc.y;
        cout << " " <<X;
        cout << " " <<minVal;
        cout << " " << maxVal;
        minVal = minVal + .5;


        int minValInt = (int)minVal;
        //cout << minValInt;
        outputs[count][0] = X;
        outputs[count][1] = minValInt;

        count = count + 1;
        if (count > *size - 1)
        {
            break;
        }
    }
    int m = outputs.size();

    // Number of columns (Assuming all rows
    // are of same size).  We can have different
    // sizes though (like Java).
    int n = outputs[0].size();

    // Displaying the 2D vector before sorting
    cout << "The Matrix before sorting is:\n";
    for (int i=0; i<m; i++)
    {
        for (int j=0; j<n ;j++)
            cout << outputs[i][j] << " ";
        cout << endl;
    }

    // Use of "sort()" for sorting on basis
    // of 1st column
    sort(outputs.begin(), outputs.end(),sortcol);


    cout << "The Matrix after sorting is:\n";
    for (int i=0; i<m; i++)
    {
        for (int j=0; j<n ;j++)
            cout << outputs[i][j] << " ";
        cout << endl;
    }

    //*************

    //average together each three entries in the second column
    int newSize = (outputs[0].size() / 3) + 1;
    cout << newSize << " ";
    vector<int> outputs2(newSize);
    int holder=0;
    count = 0;
    for (int i =0; i<*size; i+=*replicates)
    {
        for (int j=0; j<*replicates; j++)
        {
            if((i + j) < m)
            {
                cout << outputs[ i + j ][1] <<" ";
                holder += outputs[ i + j ][1];

            }
        }
        holder = holder/3;



        outputs2[count] = holder;
        count = count + 1;
    }

    //cout << outputs2 << " ";

    //nested for loop runs replicates number of times
    // outer runs for newSize times






    return outputs2;

}





//void stuff
/*
 read in image
 grayscale image
 get radius (can start with fixed value)
 get concentrations (can start with fixed value)
 get threshold (Can start with fixed value)
 get replicates (Can start with fixed with value)
 
 while (minvalue < threshold):
    find min and max values and locations
    fill in with while based on miloc and radius
    add the minVals to an array (arraylist)
    add the minLocs to an array (arraylist)
 
combine the location and value arrays
sort by the location component of the combined array
average together the replicates
 
graph against the concentrations