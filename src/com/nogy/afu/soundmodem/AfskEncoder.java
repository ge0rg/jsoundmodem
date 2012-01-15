package com.nogy.afu.soundmodem;

public class AfskEncoder {
	public static int f_low = 1200;
	public static int f_high = 2200;
	public static int bps = 1200;
	public static int samplerate = 22050;
	public static int pcmBits = 16;
	

	public static short[] encodeMessagePCM(Message m) {
		final int SAMPLES = (m.numberOfBits*samplerate)/bps;
		final int AMPLITUDE = (1 << (pcmBits-1))-1;
		short[] pcmData = new short[SAMPLES];
		int bitpos = -1;
		double cospos=0;
		int lasttone=f_low;
		for (int sample = 0; sample < SAMPLES; sample++) {
			// check if we arrived at the next bit
			if (bitpos != sample*bps/samplerate) {
				bitpos = sample*bps/samplerate;
				// if bit == 0, we need to switch 1200<->2200
				if ((m.data[bitpos/8] & (1<<bitpos%8)) == 0)
					lasttone = (lasttone==f_low)?f_high:f_low;
			}
			pcmData[sample]=(short) Math.round(Math.cos(cospos)*((1 << (pcmBits-1))-1));

			cospos += 2*Math.PI*lasttone/samplerate;
			if (cospos > 2*Math.PI)
				cospos -= 2*Math.PI;
		}
		return pcmData;
	}

}
