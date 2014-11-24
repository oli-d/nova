package com.dotc.nova.events.wrappers;

import com.dotc.nova.events.EventListener;

public interface NineParametersEventListener<ParamOneType, ParamTwoType, ParamThreeType, ParamFourType, ParamFiveType, ParamSixType, ParamSevenType, ParamEightType, ParamNineType>
extends EventListener {

	public abstract void doHandle(ParamOneType param1, ParamTwoType param2, ParamThreeType param3, ParamFourType param4,
			ParamFiveType param5, ParamSixType param6, ParamSevenType param7, ParamEightType param8, ParamNineType param9);

	@Override
	public default void handle(Object... data) {
		ParamOneType param1 = null;
		ParamTwoType param2 = null;
		ParamThreeType param3 = null;
		ParamFourType param4 = null;
		ParamFiveType param5 = null;
		ParamSixType param6 = null;
		ParamSevenType param7 = null;
		ParamEightType param8 = null;
		ParamNineType param9 = null;

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
			if (data.length > 6) {
				param7 = (ParamSevenType) data[6];
			}
			if (data.length > 7) {
				param8 = (ParamEightType) data[7];
			}
			if (data.length > 8) {
				param9 = (ParamNineType) data[8];
			}
		}

		doHandle(param1, param2, param3, param4, param5, param6, param7, param8, param9);
	}
}
