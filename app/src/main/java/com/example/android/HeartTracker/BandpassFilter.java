package com.example.android.HeartTracker;

public class BandpassFilter {
        private double[] coefficients;
        private double[] buffer;

        public BandpassFilter(int size) {
            coefficients = new double[size];
            buffer = new double[size];

            // Initialize your filter coefficients here
            // You can design the filter using methods like windowing, FIR filter design, etc.
            // For simplicity, you can set some values manually or use a library like JTransforms.
            initializeFilterCoefficients();
        }

        private void initializeFilterCoefficients() {
            // filter coefficients for 5-0.5 Hz
            coefficients[0] = 0.1;
            coefficients[1] = 0.2;
            coefficients[2] = 0.4;
            coefficients[3] = 0.2;
            coefficients[4] = 0.1;
        }

        public double filter(double input) {
            // Shift the buffer and add the new input
            for (int i = buffer.length - 1; i > 0; i--) {
                buffer[i] = buffer[i - 1];
            }
            buffer[0] = input;

            // Convolution sum
            double output = 0;
            for (int i = 0; i < coefficients.length; i++) {
                output += coefficients[i] * buffer[i];
            }

            return output;
        }
}
