package com.rainbow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rainbow.entity.Appointment;

public interface AppointmentService extends IService<Appointment> {
    Appointment getOne(Appointment appointment);
}
