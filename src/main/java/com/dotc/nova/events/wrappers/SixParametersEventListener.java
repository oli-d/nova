package com.dotc.nova.events.wrappers;

import com.dotc.nova.events.EventListener;

public abstract class SixParametersEventListener<ParamOneType, ParamTwoType, ParamThreeType, ParamFourType, ParamFiveType, ParamSixType> implements EventListener {

	abstract void handle(ParamOneType param1, ParamTwoType param2, ParamThreeType param3, ParamFourType param4, ParamFiveType param5, ParamSixType param6);

	@Override
	public void handle(Object... data) {
		ParamOneType param1 = null;
		ParamTwoType param2 = null;
		ParamThreeType param3 = null;
		ParamFourType param4 = null;
		ParamFiveType param5 = null;
		ParamSixType param6 = null;

		if (data != null) {
			if (data.length > 0) {
				param1 = (ParamOneType) data[0];
			}
			if (data.length > 1) {
				param2 = (ParamTwoType) data[1];
			}
			if (data.length > 2) {
				param3 = (ParamThreeType) data[2];
			}
			if (data.length > 3) {
				param4 = (ParamFourType) data[3];
			}
			if (data.length > 4) {
				param5 = (ParamFiveType) data[4];
			}
			if (data.length > 5) {
				param6 = (ParamSixType) data[5];
			}
		}

		handle(param1, param2, param3, param4, param5, param6);
	}
}
