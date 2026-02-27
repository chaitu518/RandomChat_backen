package com.srt.randomchat.dto;

import com.srt.randomchat.model.Gender;
import com.srt.randomchat.model.Preference;

public record JoinRequest(Gender gender, Preference preference) {
}
