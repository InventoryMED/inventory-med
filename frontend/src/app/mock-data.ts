import { Hospital } from './models';

export const INITIAL_HOSPITALS: Hospital[] = [
  {
    id: 'hospital-municipal-pronto-socorro',
    name: 'Hospital Municipal e Pronto Socorro',
    shortName: 'HMPS',
    city: 'São Paulo, SP',
    rooms: [
      {
        id: 'hmps-room-101',
        name: 'Quarto 101',
        floor: '1º andar',
        unit: 'Clínica Médica',
        beds: [
          { id: 'hmps-101-a', code: 'Leito A', status: 'AVAILABLE' },
          { id: 'hmps-101-b', code: 'Leito B', status: 'AVAILABLE' },
        ],
      },
      {
        id: 'hmps-room-301',
        name: 'Quarto 301',
        floor: '3º andar',
        unit: 'Clínica Médica',
        beds: [
          { id: 'hmps-301-a', code: 'Leito A', status: 'AVAILABLE' },
          { id: 'hmps-301-b', code: 'Leito B', status: 'AVAILABLE' },
        ],
      },
      {
        id: 'hmps-room-302',
        name: 'Quarto 302',
        floor: '3º andar',
        unit: 'Clínica Médica',
        beds: [
          { id: 'hmps-302-a', code: 'Leito A', status: 'AVAILABLE' },
          { id: 'hmps-302-b', code: 'Leito B', status: 'MAINTENANCE' },
        ],
      },
      {
        id: 'hmps-room-201',
        name: 'Quarto 201',
        floor: '2º andar',
        unit: 'Clínica Cirúrgica',
        beds: [
          { id: 'hmps-201-a', code: 'Leito A', status: 'AVAILABLE' },
          { id: 'hmps-201-b', code: 'Leito B', status: 'CLEANING' },
        ],
      },
      {
        id: 'hmps-room-202',
        name: 'Quarto 202',
        floor: '2º andar',
        unit: 'Clínica Cirúrgica',
        beds: [
          { id: 'hmps-202-a', code: 'Leito A', status: 'AVAILABLE' },
          { id: 'hmps-202-b', code: 'Leito B', status: 'AVAILABLE' },
        ],
      },
    ],
  },
  {
    id: 'hospital-municipal-pronto-atendimento',
    name: 'Hospital Municipal e Pronto Atendimento',
    shortName: 'HMPA',
    city: 'Campinas, SP',
    rooms: [
      {
        id: 'hmpa-room-101',
        name: 'Quarto 101',
        floor: '1º andar',
        unit: 'Clínica Geral',
        beds: [
          { id: 'hmpa-101-a', code: 'Leito A', status: 'AVAILABLE' },
          { id: 'hmpa-101-b', code: 'Leito B', status: 'AVAILABLE' },
        ],
      },
      {
        id: 'hmpa-room-102',
        name: 'Quarto 102',
        floor: 'Térreo',
        unit: 'Clínica Geral',
        beds: [
          { id: 'hmpa-102-a', code: 'Leito A', status: 'AVAILABLE' },
          { id: 'hmpa-102-b', code: 'Leito B', status: 'AVAILABLE' },
        ],
      },
      {
        id: 'hmpa-room-201',
        name: 'Quarto 201',
        floor: '2º andar',
        unit: 'Clínica Cirúrgica',
        beds: [
          { id: 'hmpa-201-a', code: 'Leito A', status: 'AVAILABLE' },
          { id: 'hmpa-201-b', code: 'Leito B', status: 'CLEANING' },
        ],
      },
    ],
  },
];
