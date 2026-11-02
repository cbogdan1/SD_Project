--
-- PostgreSQL database dump
--

\restrict pkQ80i9DSLpF6h4G2UY8hnJH5o1WBca2LBFcOC6CpD7BYRXQJkfGPtdddBqcn0w

-- Dumped from database version 15.19
-- Dumped by pg_dump version 15.19

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
-- Name: app_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.app_user (
    id bytea NOT NULL
);


ALTER TABLE public.app_user OWNER TO postgres;

--
-- Name: device; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.device (
    id bytea NOT NULL,
    description character varying(255) NOT NULL,
    address character varying(255) NOT NULL,
    max_energy_consumption double precision NOT NULL,
    user_id bytea,
    name character varying(255) NOT NULL
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
-- Data for Name: app_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.app_user (id) FROM stdin;
\\xa0eebc999c0b4ef8bb6d6bb9bd380a11
\\xb1eebc999c0b4ef8bb6d6bb9bd380a22
\.


--
-- Data for Name: device; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.device (id, description, address, max_energy_consumption, user_id, name) FROM stdin;
\\xd1eebc999c0b4ef8bb6d6bb9bd380a01	Fridge	Str. Observatorului 34, Cluj-Napoca	1.5	\\xa0eebc999c0b4ef8bb6d6bb9bd380a11	Fridge
\\xd2eebc999c0b4ef8bb6d6bb9bd380a02	Washing machine	Str. Observatorului 34, Cluj-Napoca	2.2	\\xa0eebc999c0b4ef8bb6d6bb9bd380a11	Washing machine
\\xd3eebc999c0b4ef8bb6d6bb9bd380a03	Air conditioner	Str. Memorandumului 10, Cluj-Napoca	3	\\xb1eebc999c0b4ef8bb6d6bb9bd380a22	Air conditioner
\\xd4eebc999c0b4ef8bb6d6bb9bd380a04	Electric heater	Str. Memorandumului 10, Cluj-Napoca	2	\N	Electric heater
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init device	SQL	V1__init_device.sql	2143846029	postgres	2026-10-26 18:17:00.193851	32	t
2	2	add device user	SQL	V2__add_device_user.sql	1378826179	postgres	2026-10-26 18:17:00.260159	10	t
3	3	insert sample devices	SQL	V3__insert_sample_devices.sql	1221267454	postgres	2026-10-26 18:17:00.295193	7	t
4	4	add device name	SQL	V4__add_device_name.sql	-108831933	postgres	2026-10-26 18:17:00.324113	8	t
5	5	add app user	SQL	V5__add_app_user.sql	-1708845662	postgres	2026-10-26 18:17:00.347434	35	t
\.


--
-- Name: app_user app_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);


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
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_device_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_device_user_id ON public.device USING btree (user_id);


--
-- Name: device fk_device_app_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.device
    ADD CONSTRAINT fk_device_app_user FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--

\unrestrict pkQ80i9DSLpF6h4G2UY8hnJH5o1WBca2LBFcOC6CpD7BYRXQJkfGPtdddBqcn0w

