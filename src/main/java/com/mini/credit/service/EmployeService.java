package com.mini.credit.service;

import com.mini.credit.dto.employe.EmployeDTO;
import com.mini.credit.dto.employe.CreateEmployeRequest;
import com.mini.credit.dto.employe.UpdateEmployeRequest;

import java.util.List;

public interface EmployeService {
    EmployeDTO create(CreateEmployeRequest request);
    EmployeDTO update(Long id, UpdateEmployeRequest request);
    EmployeDTO getById(Long id);
    EmployeDTO getByMatricule(String matricule);
    List<EmployeDTO> getAll();
    List<EmployeDTO> getAllActive();
    void delete(Long id);
}
