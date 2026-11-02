--
-- PostgreSQL database dump
--

\restrict UnxMzII3XGaiI5iopB0dE2Et9xWHsgxgybbJFVwhEsMLbeQ0eq2zZW3gm7AbeKj

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
-- Name: credential; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.credential (
    id bytea NOT NULL,
    username character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(16) NOT NULL,
    user_id bytea NOT NULL
);


ALTER TABLE public.credential OWNER TO postgres;

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
-- Data for Name: credential; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.credential (id, username, password_hash, role, user_id) FROM stdin;
\\xc1aabc999c0b4ef8bb6d6bb9bd380a11	ion	$2a$10$AxXCDaanas9zlpwGcLsfJOQhg5bhEU0LDBfzJ15FP5mJy93coMmhu	CLIENT	\\xa0eebc999c0b4ef8bb6d6bb9bd380a11
\\xc2aabc999c0b4ef8bb6d6bb9bd380a22	maria	$2a$10$CgDda4p/llVkiZPyOtY3/u.J.2ihSx5ioAnQrceOtmCYXu/qicE1a	CLIENT	\\xb1eebc999c0b4ef8bb6d6bb9bd380a22
\\x424a09bfeae84136941141b80a6e5cb7	admin	$2a$10$dYkC1MlNTwnVEQv0wEFd.eVdS.JbV2loZc83R9Yz5ppMqE/7jk1J6	ADMIN	\\xfcf90d0621de41b5a44374cd670540db
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init credential	SQL	V1__init_credential.sql	-1117866669	postgres	2026-10-26 18:17:00.026094	72	t
2	2	insert sample credentials	SQL	V2__insert_sample_credentials.sql	1124380551	postgres	2026-10-26 18:17:00.14578	4	t
\.


--
-- Name: credential credential_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credential
    ADD CONSTRAINT credential_pkey PRIMARY KEY (id);


--
-- Name: credential credential_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credential
    ADD CONSTRAINT credential_user_id_key UNIQUE (user_id);


--
-- Name: credential credential_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.credential
    ADD CONSTRAINT credential_username_key UNIQUE (username);


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
-- PostgreSQL database dump complete
--

\unrestrict UnxMzII3XGaiI5iopB0dE2Et9xWHsgxgybbJFVwhEsMLbeQ0eq2zZW3gm7AbeKj

