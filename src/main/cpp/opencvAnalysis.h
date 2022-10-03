

#ifndef OPTICALELISATEST_OPENCVANALYSIS_H
#define OPTICALELISATEST_OPENCVANALYSIS_H

#include <stdio.h>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>

using namespace cv;
using namespace std;

class opencvAnalysis {

public:

    vector<int> detect_biomarker(Mat image, int *size, int *replicates);


private:

    int b = 0;
};

#endif //OPTICALELISATEST_OPENCVANALYSIS_H
