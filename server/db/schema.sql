CREATE TABLE kanji_user (
    username character varying NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    last_login timestamp with time zone DEFAULT now() NOT NULL,
    id integer NOT NULL,
    pwhash character varying NOT NULL,
    salt character varying NOT NULL,
    admin boolean DEFAULT false NOT NULL
);

CREATE SEQUENCE user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE user_id_seq OWNED BY kanji_user.id;
ALTER TABLE ONLY kanji_user ALTER COLUMN id SET DEFAULT nextval('user_id_seq'::regclass);

ALTER TABLE ONLY kanji_user
    ADD CONSTRAINT user_pkey PRIMARY KEY (id);

ALTER TABLE ONLY kanji_user
    ADD CONSTRAINT user_username_key UNIQUE (username);