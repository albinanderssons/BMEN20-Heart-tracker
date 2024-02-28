data = load('average_red_values.txt');
avgs = data(:,1);
time = data(:,2);
deltaTs = time(2:end) - time(1:length(time) -1);
avgDeltaT = sum(deltaTs)/length(deltaTs);
%avgs = bandpass(avgs,[0.75 10/3],1/avgDeltaT);

subplot(3,1,1)
plot(time,avgs)
title("RedAVG over Time")
xlabel("t (s)")
ylabel("Average red value")
xlim([0 max(time)])

data = load('fft.txt');
f = 0:1/(avgDeltaT*(length(data) - 1)):1/avgDeltaT;
intrestingColumns = find((f>=0.75) & (f<=10/3));
f=f(intrestingColumns);
data=data(intrestingColumns);
freq = f(data == max(data));
bpm = freq * 60;
disp("App fft estimation: "+ bpm)

subplot(3,1,2)
plot(f,data);
title("App FFT")
xlabel("f (HZ)")
ylabel("PDS")

edf = ReadEDF('edf.edf');
ecg = cell2mat(edf(1));
ecg = bandpass(ecg,[0.75 1.5],1/0.001);
fft_out = fft(ecg);
PDS = fft_out .* conj(fft_out);
f = 0:1/(0.001*(length(ecg) - 1)):1/0.001;
intrestingColumns = find((f>=0.75) & (f<=10/3));
f=f(intrestingColumns);
PDS=PDS(intrestingColumns);
freq = f(PDS == max(PDS));
ecg_bpm_estimation = freq*60;
disp("ECG fft estimation: "+ ecg_bpm_estimation)
disp("diff: "+ abs(ecg_bpm_estimation-bpm))

subplot(3,1,3)
plot(f,PDS)
title("ECG FFT")
xlabel("f (Hz)")
ylabel("PDS")