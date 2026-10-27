INSERT INTO users (name) VALUES ('CPU');
INSERT INTO users (name) VALUES ('ほんだ');
INSERT INTO users (name) VALUES ('いがき');

INSERT INTO matchinfo (user1, user2, user1Hand, isActive) VALUES (1, 2, 'Gu', FALSE);
INSERT INTO matchinfo (user1, user2, user1Hand, isActive) VALUES (2, 3, 'Choki', FALSE);

INSERT INTO matches (user1, user2, user1Hand, user2Hand)
VALUES (2, 1, 'Gu', 'Choki');

INSERT INTO matches (user1, user2, user1Hand, user2Hand)
VALUES (2, 1, 'Gu', 'Gu');

INSERT INTO matches (user1, user2, user1Hand, user2Hand)
VALUES (2, 1, 'Gu', 'Pa');
