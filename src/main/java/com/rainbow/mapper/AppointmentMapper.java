package com.rainbow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rainbow.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {
}
