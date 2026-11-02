--
-- PostgreSQL database dump
--

\restrict HySkaAVa19tzBKkAkF1VdsRaJehERzf36AoNIfAyvcUBGIiF0diDFdvUOcsfWiK

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
-- Name: person; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.person (
    id bytea NOT NULL,
    name character varying(255) NOT NULL,
    address character varying(255) NOT NULL,
    age integer NOT NULL,
    username character varying(255) NOT NULL
);


ALTER TABLE public.person OWNER TO postgres;

--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init	SQL	V1__init.sql	-2075273188	postgres	2026-10-26 18:17:00.78137	26	t
2	2	insert sample persons	SQL	V2__insert_sample_persons.sql	461667616	postgres	2026-10-26 18:17:00.832182	6	t
3	3	add person username	SQL	V3__add_person_username.sql	-376810911	postgres	2026-10-26 18:17:00.855381	9	t
\.


--
-- Data for Name: person; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.person (id, name, address, age, username) FROM stdin;
\\xa0eebc999c0b4ef8bb6d6bb9bd380a11	Ion Popescu	Str. Observatorului 34, Cluj-Napoca	22	ion
\\xb1eebc999c0b4ef8bb6d6bb9bd380a22	Maria Ionescu	Str. Memorandumului 10, Cluj-Napoca	24	maria
\.


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: person person_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- PostgreSQL database dump complete
--

\unrestrict HySkaAVa19tzBKkAkF1VdsRaJehERzf36AoNIfAyvcUBGIiF0diDFdvUOcsfWiK

