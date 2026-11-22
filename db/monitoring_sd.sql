--
-- PostgreSQL database dump
--

\restrict Rw5Hp3RZxgeXzXPrzOmhYTIIpk7iwdYz2CINGPtaNYe01wWMOu8YNQK8UT96rp4

-- Dumped from database version 15.20
-- Dumped by pg_dump version 15.20

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: device; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.device (
    id bytea NOT NULL,
    user_id bytea
);


ALTER TABLE public.device OWNER TO postgres;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: hourly_consumption; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.hourly_consumption (
    id bigint NOT NULL,
    device_id bytea NOT NULL,
    hour_start timestamp without time zone NOT NULL,
    energy_kwh double precision NOT NULL
);


ALTER TABLE public.hourly_consumption OWNER TO postgres;

--
-- Name: hourly_consumption_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.hourly_consumption_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hourly_consumption_id_seq OWNER TO postgres;

--
-- Name: hourly_consumption_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.hourly_consumption_id_seq OWNED BY public.hourly_consumption.id;


--
-- Name: hourly_consumption id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.hourly_consumption ALTER COLUMN id SET DEFAULT nextval('public.hourly_consumption_id_seq'::regclass);


--
-- Data for Name: device; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.device (id, user_id) FROM stdin;
\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	\\xa0eebc999c0b4ef8bb6d6bb9bd380a11
\\xd2eebc999c0b4ef8bb6d6bb9bd380a02	\\xa0eebc999c0b4ef8bb6d6bb9bd380a11
\\xd3eebc999c0b4ef8bb6d6bb9bd380a03	\\xb1eebc999c0b4ef8bb6d6bb9bd380a22
\\xd4eebc999c0b4ef8bb6d6bb9bd380a04	\N
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init monitoring	SQL	V1__init_monitoring.sql	1563863517	postgres	2026-11-22 09:17:58.638047	146	t
2	2	insert sample devices	SQL	V2__insert_sample_devices.sql	1757852445	postgres	2026-11-22 09:17:58.812151	2	t
\.


--
-- Data for Name: hourly_consumption; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.hourly_consumption (id, device_id, hour_start, energy_kwh) FROM stdin;
1	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 00:00:00	0.2378
17	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 16:00:00	0.4123
2	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 01:00:00	0.24969999999999998
18	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 17:00:00	0.9412
3	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 02:00:00	0.24670000000000003
4	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 03:00:00	0.24469999999999997
19	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 18:00:00	0.9840999999999999
5	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 04:00:00	0.2391
20	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 19:00:00	0.9933000000000001
6	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 05:00:00	0.24659999999999999
7	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 06:00:00	0.6107999999999999
21	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 20:00:00	1.0028000000000001
8	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 07:00:00	0.6362
22	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 21:00:00	0.9973000000000001
9	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 08:00:00	0.6125
10	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 09:00:00	0.4139
23	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 22:00:00	0.5128999999999999
11	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 10:00:00	0.4244
24	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 23:00:00	0.49450000000000005
12	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 11:00:00	0.4606
13	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 12:00:00	0.4303
14	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 13:00:00	0.43890000000000007
15	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 14:00:00	0.4409
16	\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	2026-11-21 15:00:00	0.44
\.


--
-- Name: hourly_consumption_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.hourly_consumption_id_seq', 27, true);


--
-- Name: device device_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.device
    ADD CONSTRAINT device_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: hourly_consumption hourly_consumption_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.hourly_consumption
    ADD CONSTRAINT hourly_consumption_pkey PRIMARY KEY (id);


--
-- Name: hourly_consumption uq_device_hour; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.hourly_consumption
    ADD CONSTRAINT uq_device_hour UNIQUE (device_id, hour_start);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: hourly_consumption hourly_consumption_device_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.hourly_consumption
    ADD CONSTRAINT hourly_consumption_device_id_fkey FOREIGN KEY (device_id) REFERENCES public.device(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict Rw5Hp3RZxgeXzXPrzOmhYTIIpk7iwdYz2CINGPtaNYe01wWMOu8YNQK8UT96rp4

